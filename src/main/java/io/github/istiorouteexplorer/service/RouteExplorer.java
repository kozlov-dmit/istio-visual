package io.github.istiorouteexplorer.service;

import io.github.istiorouteexplorer.model.*;
import io.github.istiorouteexplorer.model.istio.*;
import io.github.istiorouteexplorer.model.kubernetes.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class RouteExplorer {

    private static final String CLUSTER_POSTFIX = ".svc.cluster.local";

    private final ResourceCollection resources;
    private final Map<String, ServiceEntryDto> serviceEntriesByHost;
//    private final List<RouteNode> meshNodes;
    private final Map<String, List<RouteNode>> gatewaysWithNodes;

    public RouteExplorer(ResourceCollection resources) {
        this.resources = resources;
        this.serviceEntriesByHost = resources.primary().getServiceEntries().stream()
                .flatMap(se -> se.getSpec().getHosts().stream()
                        .collect(Collectors.toMap(host -> host, host -> se)).entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (se1, se2) -> se1
                ));
//        this.meshNodes = findMeshNodes();
        this.gatewaysWithNodes = resources.primary().getGateways().stream()
                .collect(Collectors.toMap(
                        gw -> gw.getMetadata().getName(),
                        this::findNodeLinkedToGateway
                ));
    }

    public List<Route> buildRoutes() {
        // all nodes inside istio service mesh (also deployments, which didn't run pods)

        Map<String, Route> routeMap = new HashMap<>();
        for (VirtualServiceDto vs : resources.primary().getVirtualServices()) {
            if (vs.getSpec().getHttp() != null && !vs.getSpec().getHttp().isEmpty()) {
                // find http routes
                for (HttpRouteDto httpRoute : vs.getSpec().getHttp()) {
                    processRoutes(httpRoute, vs, routeMap);
                }
            }
            else if (vs.getSpec().getTcp() != null && !vs.getSpec().getTcp().isEmpty()) {
                for (TcpRouteDto tcpRoute : vs.getSpec().getTcp()) {
                    processRoutes(tcpRoute, vs, routeMap);
                }
            }
            else {
                for (TlsRouteDto tlsRoute : vs.getSpec().getTls()) {
                    processRoutes(tlsRoute, vs, routeMap);
                }
            }
        }

        // collect all nodes by hosts
        Map<String, List<RouteNode>> nodeMap = routeMap.values().stream()
                .flatMap(route -> route.getNodes().values().stream())
                .collect(Collectors.toMap(
                        node -> {
                            if (node.getId().contains(":")) {
                                return node.getId().substring(node.getId().indexOf(":") + 1);
                            }
                            return node.getId();
                        },
                        node -> {
                            List<RouteNode> result = new ArrayList<>();
                            result.add(node);
                            return result;
                        },
                        (nodeList1, nodeList2) -> {
                            nodeList1.addAll(nodeList2);
                            return nodeList1;
                        }
                ));

        // add destination traffic policy and workload selector to nodes
        resources.primary().getDestinationRules().forEach(dr -> {
            String host = normalizeHost(dr.getSpec().getHost(), resources.primary().getNamespace());
            List<RouteNode> nodes = nodeMap.get(host);
            if (nodes == null) {
                // if not found, try to find by host without cluster.local
                host = hostOnly(host);
                nodes = nodeMap.get(host);
            }
            if (nodes != null) {
                nodes.stream()
                        .filter(node -> dr.getSpec().getWorkloadSelector() == null || matchLabelsToSelector(node.getMetadata().getLabels(), dr.getSpec().getWorkloadSelector().getMatchLabels()))
                        .forEach(node -> {
                    if (dr.getSpec().getTrafficPolicy() != null) {
                        node.addTrafficPolicy(dr.getSpec().getTrafficPolicy());
                    }
                    if (dr.getSpec().getWorkloadSelector() != null) {
                        node.setWorkloadSelector(dr.getSpec().getWorkloadSelector());
                    }
                });
            }
        });

        return routeMap.values().stream().toList();
    }

    private void processRoutes(IstioRoute routeDto, VirtualServiceDto virtualService, Map<String, Route> routes) {
        Set<String> destinations = new HashSet<>(virtualService.getSpec().getHosts());
        List<String> gateways = routeDto.getMatch().stream()
                .flatMap(match -> match.getGateways().stream())
                .collect(Collectors.toList());
        if (gateways.isEmpty()) {
            gateways.addAll(virtualService.getSpec().getGateways());
        }
        List<Long> matchPorts = routeDto.getMatch().stream()
                .filter(TcpMatchRequestDto.class::isInstance)
                .map(match -> ((TcpMatchRequestDto)match).getPort())
                .filter(Objects::nonNull)
                .toList();
        List<MatchCondition> matchConditions = routeDto.getMatch().stream()
                .flatMap(match -> MatchCondition.fromMatch(match).stream())
                .toList();

        // find source node from virtual service http route by gateway
        List<String> sourceIds = new ArrayList<>();
        gateways.forEach(gateway -> {
            if (gateway.equals("mesh")) {
                // if gateway is mesh, add all nodes from mesh
//                sourceNodes.addAll(meshNodes);
                // use one mesh mode for all istio service mesh
                RouteNode sourceNode = new RouteNode("mesh","mesh", RouteNode.Type.MESH, new ObjectMetadataDto());
                // create routes for all destinations and for all source nodes
                destinations.forEach(destination -> {
                    Route route = routes.computeIfAbsent(destination, v -> new Route(destination, matchPorts));
                    route.addNode(sourceNode);
                    sourceIds.add(sourceNode.getId());
                });
            } else {
                gatewaysWithNodes.get(gateway).forEach(node ->
                    destinations.forEach(destination -> {
                        Route route = routes.computeIfAbsent(destination, v -> new Route(destination, matchPorts));
                        route.addNode(node);
                        sourceIds.add(node.getId());
                    })
                );
            }
            // find destination nodes
            Set<RouteNode> destinationNodes = new HashSet<>();
            routeDto.getRoute().forEach(r -> {
                // find destination node from virtual service http route
                String host = normalizeHost(r.getHost(), resources.primary().getNamespace());
                RouteNode destinationNode = findNodeByHost(host);
                if (destinationNode == null) {
                    // if not found node, create pseudo node
                    destinationNode = new RouteNode(ResourcePrefix.UNKNOWN.getPrefix() + host, host, RouteNode.Type.UNKNOWN, new ObjectMetadataDto());
                }
                destinationNodes.add(destinationNode);
                destinations.forEach(destination -> {
                    Route route = routes.get(destination);
                    if (route == null) {
                        log.error("Not found route for destination: {}", destination);
                    }
                    else {
                        // for all source nodes create route
                        sourceIds.forEach(id ->
                                destinationNodes.forEach(destNode ->
                                        route.addLink(id, destNode, "HTTP", r.getPort(), matchConditions)
                                )
                        );
                    }
                });
            });
        });


    }

    private List<RouteNode> findNodeLinkedToGateway(GatewayDto gateway) {
        // create nodes by deployments to combine all pods
        return resources.primary().getDeployments().stream()
                    .filter(deployment -> matchLabelsToSelector(deployment.getMetadata().getLabels(), gateway.getSpec().getSelector()))
                .map(deployment -> new RouteNode(
                                ResourcePrefix.DEPLOYMENT.getPrefix() + deployment.getMetadata().getName(),
                                deployment.getMetadata().getName(),
                                RouteNode.Type.DEPLOYMENT,
                                deployment.getMetadata()
                        )
                )
                .toList();
    }

    private RouteNode findNodeByHost(String host) {
        // find service entry by host from destination
        ServiceEntryDto serviceEntryDto = serviceEntriesByHost.get(host);
        if (serviceEntryDto != null) {
            return new RouteNode(ResourcePrefix.SERVICE_ENTRY.getPrefix() + host, serviceEntryDto.getMetadata().getName(), RouteNode.Type.SERVICE_ENTRY, serviceEntryDto.getMetadata());
        } else {
            // if not found service entry, it might be a service
            ServiceDto service = findServiceByHost(host);
            if (service == null) {
                log.warn("Not found service for host: {}", host);
                return null;
            }
            // trying to find deployment
            DeploymentDto deployment = findDeploymentByService(service);
            if (deployment != null) {
                return new RouteNode(ResourcePrefix.DEPLOYMENT.getPrefix() + host, deployment.getMetadata().getName(), RouteNode.Type.DEPLOYMENT, deployment.getMetadata());
            } else {
                log.warn("Not found resource for Service: {}", service.getMetadata().getName());
                return null;
            }
        }
    }

    private List<RouteNode> findMeshNodes() {
        List<RouteNode> meshNodes = new ArrayList<>();
        Set<String> existingDeploymentNames = new HashSet<>();
        resources.primary().getPods().forEach(pod -> {
            // add to mesh nodes only pods with sidecar istio-proxy
            if (pod.getMetadata().getAnnotations().getOrDefault("sidecar.istio.io/inject", "false").equalsIgnoreCase("true")) {
                meshNodes.add(new RouteNode(
                        ResourcePrefix.POD + pod.getMetadata().getName(),
                        pod.getMetadata().getName(),
                        RouteNode.Type.POD,
                        pod.getMetadata()
                ));
                DeploymentDto ownerDeployment = findPodOwnerDeployment(pod);
                if (ownerDeployment != null) {
                    existingDeploymentNames.add(ownerDeployment.getMetadata().getName());
                }
            }
        });
        resources.primary().getDeployments().stream()
                .filter(deployment -> !existingDeploymentNames.contains(deployment.getMetadata().getName()))
                .forEach(deployment ->
                    meshNodes.add(new RouteNode(
                            ResourcePrefix.DEPLOYMENT + deployment.getMetadata().getName(),
                            deployment.getMetadata().getName(),
                            RouteNode.Type.DEPLOYMENT,
                            deployment.getMetadata()
                    ))
                );

        return meshNodes;
    }

    private DeploymentDto findPodOwnerDeployment(PodDto pod) {
        OwnerReferenceDto podOwnerDto = pod.getMetadata().getOwnerReferences().stream()
                .filter(owner -> "ReplicaSet".equalsIgnoreCase(owner.getKind()))
                .findFirst()
                .orElse(null);
        if (podOwnerDto == null || podOwnerDto.getName() == null) {
            log.warn("Not found owner for pod: {}", pod.getMetadata().getName());
            return null;
        }
        ReplicaSetDto replicaSetOwner = resources.primary().getReplicaSets().stream()
                .filter(rs -> podOwnerDto.getName().equalsIgnoreCase(rs.getMetadata().getName()))
                .findFirst()
                .orElse(null);
        if (replicaSetOwner == null) {
            log.warn("Not found owner for pod: {}", pod.getMetadata().getName());
            return null;
        }
        OwnerReferenceDto rsOwnerDto = replicaSetOwner.getMetadata().getOwnerReferences().stream()
                .filter(owner -> "Deployment".equalsIgnoreCase(owner.getKind()))
                .findFirst()
                .orElse(null);
        if (rsOwnerDto == null || rsOwnerDto.getName() == null) {
            log.warn("Not found owner for replicaSet: {}", replicaSetOwner.getMetadata().getName());
            return null;
        }
        DeploymentDto deploymentOwner = resources.primary().getDeployments().stream()
                .filter(deployment -> rsOwnerDto.getName().equalsIgnoreCase(deployment.getMetadata().getName()))
                .findFirst()
                .orElse(null);
        if (deploymentOwner == null) {
            log.warn("Not found owner for replicaSet: {}", replicaSetOwner.getMetadata().getName());
            return null;
        }
        return deploymentOwner;
    }

    private ServiceDto findServiceByHost(String host) {
        return resources.primary().getServices().stream()
                .filter(s -> matchHost(host, s.getMetadata().getName()))
                .findFirst()
                .orElse(null);
    }

    private boolean matchHost(String host, String targetName) {
        if (host.equalsIgnoreCase(targetName)) {
            return true;
        }
        // try match without <namespace>.svc.cluster.local
        if (host.endsWith("." + resources.primary().getNamespace() + CLUSTER_POSTFIX)) {
            return host.substring(0, ("." + resources.primary().getNamespace() + CLUSTER_POSTFIX).length()).equalsIgnoreCase(targetName);
        } else {
            return false;
        }
    }

    private DeploymentDto findDeploymentByService(ServiceDto serviceDto) {
        return resources.primary().getDeployments().stream()
                .filter(deployment -> matchLabelsToSelector(deployment.getMetadata().getLabels(), serviceDto.getSpec().getSelector()))
                .findFirst()
                .orElse(null);
    }

    private boolean matchLabelsToSelector(Map<String, String> labels, Map<String, String> selector) {
        // check if all selector values are equal to labels
        for (Map.Entry<String, String> selectorEntry : selector.entrySet()) {
            if (!selectorEntry.getValue().equalsIgnoreCase(labels.get(selectorEntry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    private boolean match(ServiceEntryDto serviceEntry, StringMatchDto matchDto) {
        if (matchDto.getExact() != null) {
            return serviceEntry.getSpec().getHosts().stream().allMatch(host -> host.equals(matchDto.getExact()));
        } else if (matchDto.getPrefix() != null) {
            return serviceEntry.getSpec().getHosts().stream().allMatch(host -> host.startsWith(matchDto.getPrefix()));
        } else if (matchDto.getRegex() != null) {
            return serviceEntry.getSpec().getHosts().stream().allMatch(host -> host.matches(matchDto.getRegex()));
        } else {
            return false;
        }
    }

    private String normalizeHost(String host, String namespace) {
        if (host == null) {
            return "";
        }
        host = host.trim();
        if (host.contains(".")) {
            // host is fqdn
            return host;
        }
        return fqdn(host, namespace);
    }

    private String fqdn(String svcName, String namespace) {
        return svcName + "." + namespace + CLUSTER_POSTFIX;
    }

    private String hostOnly(String fqdn) {
        if (fqdn.contains(".")) {
            return fqdn.substring(0, fqdn.indexOf("."));
        } else {
            return fqdn;
        }
    }
}

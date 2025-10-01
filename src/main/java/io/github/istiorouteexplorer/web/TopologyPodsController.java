package io.github.istiorouteexplorer.web;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Lightweight controller to provide pods grouped by service and service ports.
 * Endpoint:
 * GET /api/topology/pods?namespace=<ns>
 *
 * Response:
 * {
 * podsByService: { "svc-name": [ { name, phase, nodeName }, ... ] },
 * services: [ { name, ports: [ { name, port, protocol } ] } ]
 * }
 *
 * Best-effort: uses Endpoints -> endpoint subsets -> targetRef to find pods for
 * service.
 */
@Slf4j
@RestController
@RequestMapping("/api/topology")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TopologyPodsController {

    private final KubernetesClient k8s;

    @GetMapping("/pods")
    public Map<String, Object> pods(@RequestParam(defaultValue = "default") String namespace) {
        Map<String, List<Map<String, Object>>> podsByService = new HashMap<>();
        List<Map<String, Object>> servicesList = new ArrayList<>();

        try {
            ServiceList sl = k8s.services().inNamespace(namespace).list();
            List<Service> svcs = sl == null ? Collections.emptyList() : sl.getItems();
            for (Service s : svcs) {
                String svcName = Optional.ofNullable(s.getMetadata()).map(m -> m.getName()).orElse("<unknown>");
                List<Map<String, Object>> ports = Optional.ofNullable(s.getSpec())
                        .map(spec -> Optional.ofNullable(spec.getPorts()).orElse(Collections.emptyList())
                                .stream()
                                .map(p -> Map.<String, Object>of("name", p.getName(), "port", p.getPort(), "protocol", p.getProtocol()))
                                .collect(Collectors.toList()))
                        .orElse(Collections.emptyList());
                servicesList.add(Map.of("name", svcName, "ports", ports));
            }
        } catch (Exception ex) {
            // ignore — best-effort
            log.error("Exception while get pods from kubernetes", ex);
        }

        try {
            // Use Endpoints to map service->pod targetRefs
            EndpointsList eps = k8s.endpoints().inNamespace(namespace).list();
            List<Endpoints> endpoints = eps == null ? Collections.emptyList() : eps.getItems();
            PodList podList = k8s.pods().inNamespace(namespace).list();
            List<Pod> allPods = podList == null ? Collections.emptyList() : podList.getItems();

            for (Endpoints ep : endpoints) {
                String svcName = Optional.ofNullable(ep.getMetadata()).map(m -> m.getName()).orElse("<unknown>");
                List<Map<String, Object>> pods = new ArrayList<>();
                if (ep.getSubsets() != null) {
                    ep.getSubsets().forEach(sub -> {
                        if (sub.getAddresses() != null) {
                            sub.getAddresses().forEach(addr -> {
                                if (addr.getTargetRef() != null && "Pod".equals(addr.getTargetRef().getKind())) {
                                    String podName = addr.getTargetRef().getName();
                                    // find pod metadata
                                    Optional<Pod> podOpt = allPods.stream().filter(
                                            p -> p.getMetadata() != null && podName.equals(p.getMetadata().getName()))
                                            .findFirst();
                                    if (podOpt.isPresent()) {
                                        Pod p = podOpt.get();
                                        Map<String, Object> info = Map.of(
                                                "name", podName,
                                                "phase",
                                                Optional.ofNullable(p.getStatus()).map(st -> st.getPhase())
                                                        .orElse("Unknown"),
                                                "nodeName", Optional.ofNullable(p.getSpec()).map(sp -> sp.getNodeName())
                                                        .orElse(null));
                                        pods.add(info);
                                    } else {
                                        pods.add(Map.of("name", podName));
                                    }
                                }
                            });
                        }
                    });
                }
                podsByService.put(svcName, pods);
            }
        } catch (Exception ex) {
            // best-effort: fallback to empty
            log.error("Exception while get endpoints from kubernetes", ex);
        }

        return Map.of("podsByService", podsByService, "services", servicesList);
    }
}

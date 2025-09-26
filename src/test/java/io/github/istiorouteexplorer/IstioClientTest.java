package io.github.istiorouteexplorer;

import io.fabric8.istio.api.networking.v1beta1.DestinationRuleList;
import io.fabric8.istio.api.networking.v1beta1.GatewayList;
import io.fabric8.istio.api.networking.v1beta1.ServiceEntryList;
import io.fabric8.istio.api.networking.v1beta1.VirtualServiceList;
import io.fabric8.istio.client.DefaultIstioClient;
import io.fabric8.istio.client.IstioClient;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;

@Slf4j
public class IstioClientTest {

    @Test
    public void test() {
        String namespace = "fort-istio";
        String kubeConfigPath = Optional.ofNullable(System.getProperty("istio.test.kubeconfig"))
                .orElse(System.getenv("ISTIO_TEST_KUBECONFIG"));
        Assumptions.assumeTrue(kubeConfigPath != null && !kubeConfigPath.isBlank(),
                "No kubeconfig path configured for IstioClientTest");
        File kubeConfig = new File(kubeConfigPath);
        Assumptions.assumeTrue(kubeConfig.exists(),
                "Configured kubeconfig path does not exist: " + kubeConfig.getAbsolutePath());
        KubernetesClient kubernetesClient = new KubernetesClientBuilder()
                .withConfig(Config.fromKubeconfig(kubeConfig))
                .build();
        IstioClient istioClient = new DefaultIstioClient(kubernetesClient);
        VirtualServiceList virtualServiceList = istioClient.v1beta1().virtualServices().inNamespace(namespace).list();
        log.info("VirtualServices: {}", virtualServiceList.getItems().size());
        DestinationRuleList destinationRuleList = istioClient.v1beta1().destinationRules().inNamespace(namespace).list();
        log.info("DestinationRules: {}", destinationRuleList.getItems().size());
        ServiceEntryList serviceEntryList = istioClient.v1beta1().serviceEntries().inNamespace(namespace).list();
        log.info("ServiceEntries: {}", serviceEntryList.getItems().size());
        GatewayList gatewayList = istioClient.v1beta1().gateways().inNamespace(namespace).list();
        log.info("Gateways: {}", gatewayList.getItems().size());
        ServiceList serviceList = kubernetesClient.services().inNamespace(namespace).list();
        log.info("Services: {}", serviceList.getItems().size());
        PodList podList = kubernetesClient.pods().inNamespace(namespace).list();
        log.info("Pods: {}", podList.getItems().size());

        Map<HostLabels, IstioHost> hosts = new HashMap<>();
        serviceList.getItems().forEach(service -> {
            HostLabels labels = new HostLabels(service.getSpec().getSelector());
            service.getSpec().getPorts().forEach(port -> {
                // add host to map if not exists
                if (!hosts.containsKey(labels)) {
                    hosts.put(
                            labels,
                            new IstioHost(
                                    labels,
                                    new ArrayList<>(),
                                    new IstioRule()
                            )
                    );
                }
                // add port to host
                hosts.get(labels).ports().add(new IstioPort(port.getPort(), port.getProtocol(), port.getName()));
            });
        });

        for (IstioHost host : hosts.values()) {
            log.info("Host: {}", host);
        }

        Map<String, IstioDestination> destinations = new HashMap<>();
        serviceEntryList.getItems().forEach(serviceEntry -> {
            // find all ports for service
            List<IstioPort> ports = serviceEntry.getSpec().getPorts().stream()
                    .map(port -> new IstioPort(port.getNumber(), port.getProtocol(), port.getName()))
                    .toList();
            serviceEntry.getSpec().getHosts().forEach(host -> {
                // for every host find or create destination and add ports
                if (!destinations.containsKey(host)) {
                    destinations.put(
                            host,
                            new IstioDestination(
                                    host,
                                    new HashMap<>()
                            )
                    );
                }
                ports.forEach(port -> destinations.get(host).ports().putIfAbsent(port.port(), port));
            });
        });

        for (IstioDestination destination : destinations.values()) {
            log.info("Destination: {}", destination);
        }

//        List<IstioRoute> routes = new ArrayList<>();
//        virtualServiceList.getItems().forEach(virtualService -> {
//            virtualService.getSpec().getHttp().forEach(http -> {
//                http.getMatch().forEach(match -> {})
//            });
//        });

    }

    /**
     * ??????> ????????????????? ?? Istio
     *
     * @param host ?:?????' ?????????????????
     * @param ports ???????'?< ?????????????????
     */
    record IstioDestination(String host, Map<Number, IstioPort> ports) {
    }

    /**
     * ?????????????'
     */
    @RequiredArgsConstructor
    static class IstioRoute {
        final LinkedList<IstioHost> route;

        public IstioRoute() {
            this.route = new LinkedList<>();
        }
    }

    /**
     * ??????> ?? Istio ??????? ????'?????<?? ???????:??????' ?'?????"???
     *
     * @param labels ????'??? ?????>??
     * @param ports  ???:???????%??? ???????'?<
     * @param rule   ?????????????? ??????????> ?:?????'??
     */
    record IstioHost(HostLabels labels, List<IstioPort> ports, IstioRule rule) {
    }

    /**
     * ?????????????? ??????????>??
     */
    record IstioRule() {
    }

    record IstioPort(Number port, String protocol, String name) {
    }

    @ToString
    @RequiredArgsConstructor
    static class HostLabels {
        final Map<String, String> labels;

        public HostLabels() {
            this.labels = new HashMap<>();
        }

        public boolean equals(HostLabels other) {
            // if all labels from other are in this and equals, then true
            for (Map.Entry<String, String> entry : labels.entrySet()) {
                if (!labels.containsKey(entry.getKey()) || !labels.get(entry.getKey()).equals(entry.getValue())) {
                    return false;
                }
            }
            return true;
        }

        public int hashCode() {
            if (labels == null) {
                return 0;
            } else {
                return labels.hashCode();
            }
        }
    }
}

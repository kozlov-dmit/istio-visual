package io.github.istiorouteexplorer.web;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import io.fabric8.istio.api.networking.v1beta1.Gateway;
import io.fabric8.istio.api.networking.v1beta1.ServiceEntry;
import io.fabric8.istio.api.networking.v1beta1.VirtualService;
import io.fabric8.istio.client.DefaultIstioClient;
import io.fabric8.istio.client.IstioClient;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/topology")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TopologyMeshController {

    private final KubernetesClient k8s;
    private final IstioClient istio;

    /**
     * Returns a mesh-oriented snapshot for the given namespace:
     * pods (only those that have an istio sidecar), services (with selectors), virtualservices, gateways, serviceentries.
     */
    @GetMapping("/mesh")
    public Map<String,Object> mesh(@RequestParam String namespace) {
        Map<String,Object> out = new LinkedHashMap<>();

        // pods with sidecar
        List<Map<String,Object>> pods = new ArrayList<>();
        try {
            var podList = k8s.pods().inNamespace(namespace).list().getItems();
            for (Pod p : podList) {
                boolean hasSidecar = false;
                if (p.getSpec() != null && p.getSpec().getContainers() != null) {
                    hasSidecar = p.getSpec().getContainers().stream().anyMatch(c -> c.getName() != null && c.getName().contains("istio-proxy"));
                }
                Map<String,String> labels = p.getMetadata() != null && p.getMetadata().getLabels() != null ? p.getMetadata().getLabels() : Collections.emptyMap();
                pods.add(Map.of("name", p.getMetadata().getName(), "labels", labels, "hasSidecar", hasSidecar));
            }
        } catch (Exception ex) { /* ignore */ }
        out.put("pods", pods);

        // services and selectors
        List<Map<String,Object>> services = new ArrayList<>();
        try {
            var svcList = k8s.services().inNamespace(namespace).list().getItems();
            for (Service s : svcList) {
                Map<String,String> selector = s.getSpec() != null && s.getSpec().getSelector() != null ? s.getSpec().getSelector() : Collections.emptyMap();
                services.add(Map.of("name", s.getMetadata().getName(), "selector", selector));
            }
        } catch (Exception ex) { }
        out.put("services", services);

        // virtualservices
        List<Map<String,Object>> virtualServices = new ArrayList<>();
        try {
            var vsList = istio.v1beta1().virtualServices().inNamespace(namespace).list().getItems();
            for (VirtualService vs : vsList) {
                Map<String,Object> vsMap = new LinkedHashMap<>();
                vsMap.put("name", Optional.ofNullable(vs.getMetadata()).map(m->m.getName()).orElse("<vs>"));
                vsMap.put("hosts", Optional.ofNullable(vs.getSpec()).map(s->s.getHosts()).orElse(Collections.emptyList()));
                vsMap.put("gateways", Optional.ofNullable(vs.getSpec()).map(s->Optional.ofNullable(s.getGateways()).orElse(Collections.emptyList())).orElse(Collections.emptyList()));

                List<Map<String,Object>> routes = new ArrayList<>();
                if (vs.getSpec() != null) {
                    if (vs.getSpec().getHttp() != null) {
                        for (var http : vs.getSpec().getHttp()) {
                            if (http.getRoute() != null) {
                                for (var r : http.getRoute()) {
                                    var d = r.getDestination();
                                    if (d != null) {
                                        routes.add(Map.of("host", d.getHost(), "port", d.getPort()!=null?d.getPort().getNumber():null));
                                    }
                                }
                            }
                        }
                    }
                    if (vs.getSpec().getTcp() != null) {
                        for (var tcp : vs.getSpec().getTcp()) {
                            if (tcp.getRoute() != null) for (var r : tcp.getRoute()) {
                                var d = r.getDestination(); if (d!=null) routes.add(Map.of("host", d.getHost(), "port", d.getPort()!=null?d.getPort().getNumber():null));
                            }
                        }
                    }
                }
                vsMap.put("routes", routes);
                virtualServices.add(vsMap);
            }
        } catch (Exception ex) { }
        out.put("virtualServices", virtualServices);

        // gateways
        List<Map<String,Object>> gateways = new ArrayList<>();
        try {
            var gwList = istio.v1beta1().gateways().inNamespace(namespace).list().getItems();
            for (Gateway g : gwList) {
                Map<String,String> selector = g.getSpec()!=null && g.getSpec().getSelector()!=null ? g.getSpec().getSelector() : Collections.emptyMap();
                gateways.add(Map.of("name", Optional.ofNullable(g.getMetadata()).map(m->m.getName()).orElse("<gw>"), "selector", selector));
            }
        } catch (Exception ex) { }
        out.put("gateways", gateways);

        // service entries (all namespaces)
        List<Map<String,Object>> serviceEntries = new ArrayList<>();
        try {
            var seList = istio.v1beta1().serviceEntries().inNamespace(namespace).list().getItems();
            for (ServiceEntry se : seList) {
                serviceEntries.add(Map.of("name", Optional.ofNullable(se.getMetadata()).map(m->m.getName()).orElse("<se>"), "hosts", Optional.ofNullable(se.getSpec()).map(s->Optional.ofNullable(s.getHosts()).orElse(Collections.emptyList())).orElse(Collections.emptyList()), "ports", Optional.ofNullable(se.getSpec()).map(s->Optional.ofNullable(s.getPorts()).orElse(Collections.emptyList())).orElse(Collections.emptyList())));
            }
        } catch (Exception ex) { }
        out.put("serviceEntries", serviceEntries);

        return out;
    }
}

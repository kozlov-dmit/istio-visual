package io.github.istiorouteexplorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.fabric8.istio.client.DefaultIstioClient;
import io.fabric8.istio.client.IstioClient;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.github.istiorouteexplorer.config.KubernetesClientConfig;
import io.github.istiorouteexplorer.kube.IstioResourceLoader;
import io.github.istiorouteexplorer.model.ResourceCollection;
import io.github.istiorouteexplorer.model.Route;
import io.github.istiorouteexplorer.model.RouteNode;
import io.github.istiorouteexplorer.service.RouteExplorer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
class IstioClientTest {

    @Test
    void test() throws Exception {
        String namespace = "fort-istio";
        File kubeConfig = new File("/Users/01571422/Documents/DEVELOP/kube-config/ift/ape6pntk-kubeconfig.txt");
        Assumptions.assumeTrue(kubeConfig.exists(),
                "Configured kubeconfig path does not exist: " + kubeConfig.getAbsolutePath());
        KubernetesClient kubernetesClient = new KubernetesClientBuilder()
                .withConfig(Config.fromKubeconfig(kubeConfig))
                .build();
        IstioClient istioClient = new DefaultIstioClient(kubernetesClient);

        ModelMapper modelMapper = new KubernetesClientConfig().modelMapper();
        IstioResourceLoader loader = new IstioResourceLoader(kubernetesClient, istioClient, modelMapper);
        ResourceCollection resourceCollection = loader.load("fort-istio", List.of());

        RouteExplorer explorer = new RouteExplorer(resourceCollection);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.writeValue(new File("namespace-resources.json"), resourceCollection.primary());

        List<Route> routes = explorer.buildRoutes();

        routes.forEach(route -> {
            StringBuilder builder = new StringBuilder("Route to " + route.getDestinationHost() + ":" + String.join(",", route.getDestinationPorts().stream().map(Object::toString).toList()) + ": ");
            AtomicBoolean first = new AtomicBoolean(true);
            route.getLinks().forEach(link -> {
                if (first.get()) {
                    RouteNode from = route.getNodes().get(link.getFromId());
                    builder.append(from.getId());
                    first.set(false);
                }
                RouteNode to = route.getNodes().get(link.getToId());
                builder.append(" -> ")
                        .append("(")
                        .append(link.getProtocol())
                        .append(":")
                        .append(link.getPort())
                        .append(")")
                        .append(" -> ")
                        .append(to.getId());
            });
            log.info(builder.toString());
        });

    }


}

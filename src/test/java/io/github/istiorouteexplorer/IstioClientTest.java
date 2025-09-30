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
import io.github.istiorouteexplorer.config.KubernetesClientConfig;
import io.github.istiorouteexplorer.kube.IstioResourceLoader;
import io.github.istiorouteexplorer.model.ResourceCollection;
import io.github.istiorouteexplorer.model.istio.IstioRoute;
import io.github.istiorouteexplorer.model.istio.RouteDestinationDto;
import io.github.istiorouteexplorer.model.istio.VirtualServiceDto;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.io.File;
import java.util.*;

@Slf4j
public class IstioClientTest {

//     @Test
//     public void test() throws Exception {
//         String namespace = "fort-istio";
//         String kubeConfigPath = Optional.ofNullable(System.getProperty("istio.test.kubeconfig"))
//                 .orElse(System.getenv("ISTIO_TEST_KUBECONFIG"));
//         Assumptions.assumeTrue(kubeConfigPath != null && !kubeConfigPath.isBlank(),
//                 "No kubeconfig path configured for IstioClientTest");
//         File kubeConfig = new File(kubeConfigPath);
//         Assumptions.assumeTrue(kubeConfig.exists(),
//                 "Configured kubeconfig path does not exist: " + kubeConfig.getAbsolutePath());
//         KubernetesClient kubernetesClient = new KubernetesClientBuilder()
//                 .withConfig(Config.fromKubeconfig(kubeConfig))
//                 .build();
//         IstioClient istioClient = new DefaultIstioClient(kubernetesClient);

//         ModelMapper modelMapper = new KubernetesClientConfig().modelMapper();
//         IstioResourceLoader loader = new IstioResourceLoader(kubernetesClient, istioClient, modelMapper);
//         ResourceCollection resourceCollection = loader.load("fort-istio", List.of());

//         for (VirtualServiceDto vs : resourceCollection.primary().virtualServices()) {
//             List<String> hosts = vs.spec().hosts();
//             List<IstioRoute> routes = new ArrayList<>(vs.spec().http());
//             routes.addAll(vs.spec().tcp());
//             routes.addAll(vs.spec().tls());

//         }

//     }

}

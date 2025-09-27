package io.github.istiorouteexplorer.config;

import io.fabric8.istio.api.api.networking.v1alpha3.*;
import io.fabric8.istio.api.networking.v1beta1.*;
import io.fabric8.istio.client.IstioClient;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import io.github.istiorouteexplorer.model.istio.*;
import io.github.istiorouteexplorer.model.kubernetes.*;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KubernetesClientConfig {

    private static final Logger log = LoggerFactory.getLogger(KubernetesClientConfig.class);

    @Bean
    public Config fabric8Config(AppProperties properties) throws IOException {
        Config baseConfig;
        String kubeConfigPath = properties.getKubeConfig();
        if (kubeConfigPath != null && !kubeConfigPath.isBlank()) {
            Path path = Path.of(kubeConfigPath);
            log.info("Loading kubeconfig from {}", path.toAbsolutePath());
            String kubeConfigContent = Files.readString(path);
            baseConfig = Config.fromKubeconfig(kubeConfigContent);
        } else {
            log.info("Using automatic Kubernetes configuration (in-cluster or default kubeconfig)");
            baseConfig = Config.autoConfigure(null);
        }
        ConfigBuilder builder = new ConfigBuilder(baseConfig);
        applyTimeout(builder, properties.getRequestTimeout());
        builder.withTrustCerts(properties.isSkipTlsVerify());
        return builder.build();
    }

    private void applyTimeout(ConfigBuilder builder, Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            return;
        }
        int millis = (int) Math.min(Integer.MAX_VALUE, timeout.toMillis());
        builder.withRequestTimeout(millis);
        builder.withConnectionTimeout(millis);
    }

    @Bean(destroyMethod = "close")
    public KubernetesClient kubernetesClient(Config config) {
        return new KubernetesClientBuilder().withConfig(config).build();
    }

    @Bean
    public IstioClient istioClient(KubernetesClient kubernetesClient) {
        return kubernetesClient.adapt(IstioClient.class);
    }

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(AccessLevel.PRIVATE)
                .setSkipNullEnabled(true);

        modelMapper.createTypeMap(ObjectMeta.class, ObjectMetadataDto.class);
        modelMapper.createTypeMap(Container.class, ContainerDto.class);
        modelMapper.createTypeMap(PodSpec.class, PodSpecDto.class);
        modelMapper.createTypeMap(PodStatus.class, PodStatusDto.class);
        modelMapper.createTypeMap(Pod.class, PodDto.class);
        modelMapper.createTypeMap(io.fabric8.istio.api.networking.v1beta1.VirtualService.class, VirtualServiceDto.class);
        modelMapper.createTypeMap(io.fabric8.istio.api.api.networking.v1alpha3.VirtualService.class, VirtualServiceSpecDto.class);
        modelMapper.createTypeMap(io.fabric8.istio.api.api.networking.v1alpha3.HTTPRoute.class, HttpRouteDto.class);
        modelMapper.createTypeMap(io.fabric8.istio.api.api.networking.v1alpha3.HTTPRouteDestination.class, HttpRouteDestinationDto.class);
        modelMapper.createTypeMap(io.fabric8.istio.api.api.networking.v1alpha3.Destination.class, DestinationDto.class);
        modelMapper.createTypeMap(io.fabric8.istio.api.api.networking.v1alpha3.HTTPMirrorPolicy.class, HttpMirrorDto.class);
        modelMapper.createTypeMap(io.fabric8.istio.api.api.networking.v1alpha3.TCPRoute.class, TcpRouteDto.class);
        modelMapper.createTypeMap(io.fabric8.istio.api.api.networking.v1alpha3.TLSRoute.class, TlsRouteDto.class);
        modelMapper.createTypeMap(io.fabric8.istio.api.networking.v1beta1.DestinationRule.class, DestinationRuleDto.class);
        modelMapper.createTypeMap(io.fabric8.istio.api.api.networking.v1alpha3.DestinationRule.class, DestinationRuleSpecDto.class);
        modelMapper.createTypeMap(io.fabric8.istio.api.api.networking.v1alpha3.TrafficPolicy.class, TrafficPolicyDto.class);
        modelMapper.createTypeMap(io.fabric8.istio.api.api.networking.v1alpha3.ClientTLSSettings.class, ClientTlsSettingsDto.class);
        modelMapper.createTypeMap(io.fabric8.istio.api.api.networking.v1alpha3.ServerTLSSettings.class, ServerTlsSettingsDto.class);
        modelMapper.createTypeMap(io.fabric8.istio.api.api.networking.v1alpha3.Subset.class, SubsetDto.class);
        modelMapper.createTypeMap(io.fabric8.istio.api.networking.v1beta1.ServiceEntry.class, ServiceEntryDto.class);
        modelMapper.createTypeMap(io.fabric8.istio.api.api.networking.v1alpha3.ServiceEntry.class, ServiceEntrySpecDto.class);
        modelMapper.createTypeMap(io.fabric8.istio.api.networking.v1beta1.WorkloadEntry.class, WorkloadEntryDto.class);
        modelMapper.createTypeMap(io.fabric8.istio.api.api.networking.v1alpha3.WorkloadEntry.class, WorkloadEntrySpecDto.class);
        modelMapper.createTypeMap(io.fabric8.istio.api.networking.v1beta1.Gateway.class, GatewayDto.class);
        modelMapper.createTypeMap(io.fabric8.istio.api.api.networking.v1alpha3.Gateway.class, GatewaySpecDto.class);
        modelMapper.createTypeMap(io.fabric8.istio.api.api.networking.v1alpha3.Server.class, ServerDto.class);
        return modelMapper;
    }

}

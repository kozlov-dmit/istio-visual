package io.github.istiorouteexplorer.config;

import io.fabric8.istio.client.IstioClient;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
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
}

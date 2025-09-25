package io.github.istiorouteexplorer.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KubernetesClientConfig {

    private static final Logger log = LoggerFactory.getLogger(KubernetesClientConfig.class);

    @Bean
    public ApiClient apiClient(AppProperties properties) throws IOException {
        ApiClient client;
        String kubeConfigPath = properties.getKubeConfig();
        if (kubeConfigPath != null && !kubeConfigPath.isBlank()) {
            File file = new File(kubeConfigPath);
            log.info("Loading kubeconfig from {}", file.getAbsolutePath());
            try (FileReader reader = new FileReader(file)) {
                KubeConfig kubeConfig = KubeConfig.loadKubeConfig(reader);
                client = ClientBuilder.kubeconfig(kubeConfig).build();
            }
        } else {
            log.info("Using in-cluster Kubernetes configuration");
            client = ClientBuilder.standard().build();
        }
        configureTimeouts(client, properties.getRequestTimeout());
        client.setVerifyingSsl(!properties.isSkipTlsVerify());
        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
        return client;
    }

    private void configureTimeouts(ApiClient client, Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            return;
        }
        int millis = Math.toIntExact(Math.min(Integer.MAX_VALUE, timeout.toMillis()));
        client.setConnectTimeout(millis);
        client.setReadTimeout(millis);
        client.setWriteTimeout(millis);
    }

    @Bean
    public CoreV1Api coreV1Api(ApiClient client) {
        return new CoreV1Api(client);
    }

    @Bean
    public CustomObjectsApi customObjectsApi(ApiClient client) {
        return new CustomObjectsApi(client);
    }
}


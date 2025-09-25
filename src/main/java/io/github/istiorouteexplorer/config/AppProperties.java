package io.github.istiorouteexplorer.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /**
     * Default namespace to inspect when none is provided via the query parameter.
     */
    private String namespace = "default";

    /**
     * Path to a kubeconfig file. When empty the in-cluster configuration is used.
     */
    private String kubeConfig = "";

    /**
     * Additional namespaces to include when gathering shared Istio resources.
     */
    private List<String> extraNamespaces = new ArrayList<>();

    /**
     * Cache TTL for rendered graphs.
     */
    private Duration cacheTtl = Duration.ofSeconds(15);

    /**
     * Timeout when calling the Kubernetes API.
     */
    private Duration requestTimeout = Duration.ofSeconds(10);

    /**
     * Skip TLS verification when talking to the Kubernetes API.
     */
    private boolean skipTlsVerify = false;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getKubeConfig() {
        return kubeConfig;
    }

    public void setKubeConfig(String kubeConfig) {
        this.kubeConfig = kubeConfig;
    }

    public List<String> getExtraNamespaces() {
        return extraNamespaces;
    }

    public void setExtraNamespaces(List<String> extraNamespaces) {
        this.extraNamespaces = extraNamespaces;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public boolean isSkipTlsVerify() {
        return skipTlsVerify;
    }

    public void setSkipTlsVerify(boolean skipTlsVerify) {
        this.skipTlsVerify = skipTlsVerify;
    }
}

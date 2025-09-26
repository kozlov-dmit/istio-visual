package io.github.istiorouteexplorer.model.istio;

/**
 * TLS modes enforced by the proxy
 */
public enum ServerTlsMode {
    AUTO_PASSTHROUGH(3),
    ISTIO_MUTUAL(4),
    MUTUAL(2),
    OPTIONAL_MUTUAL(5),
    PASSTHROUGH(0),
    SIMPLE(1);
    private final int value;

    private ServerTlsMode(final int value) {
        this.value = value;
    }
}

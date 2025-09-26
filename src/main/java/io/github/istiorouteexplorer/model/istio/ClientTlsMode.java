package io.github.istiorouteexplorer.model.istio;

public enum ClientTlsMode {
    DISABLE(0),
    ISTIO_MUTUAL(3),
    MUTUAL(2),
    SIMPLE(1);
    private final int value;
    ClientTlsMode(int value) {
        this.value = value;
    }
}

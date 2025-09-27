package io.github.istiorouteexplorer.model.istio;

/**
 * Resolution determines how the proxy will resolve the IP addresses of the network endpoints associated with the service, so that it can route to one of them
 */
public enum ServiceEntryResolution {
    DNS(2),
    DNS_ROUND_ROBIN(3),
    NONE(0),
    STATIC(1);
    private int value;
    ServiceEntryResolution(int value) {
        this.value = value;
    }
}

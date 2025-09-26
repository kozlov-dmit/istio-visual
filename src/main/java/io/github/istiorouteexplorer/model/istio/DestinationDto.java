package io.github.istiorouteexplorer.model.istio;

import io.fabric8.istio.api.api.networking.v1alpha3.PortSelector;
import java.util.Objects;

/**
 * Istio Destination
 * @param host host
 * @param port port
 * @param subset subset
 */

public final class DestinationDto {

    private final String host;
    private final Long port;
    private final String subset;

    public DestinationDto(String host, Long port, String subset) {
        this.host = host;
        this.port = port;
        this.subset = subset;
    }

    public String host() {
        return host;
    }

    public Long port() {
        return port;
    }

    public String subset() {
        return subset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DestinationDto that)) {
            return false;
        }
        return Objects.equals(host, that.host) &&
            Objects.equals(port, that.port) &&
            Objects.equals(subset, that.subset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, subset);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("DestinationDto{");
        builder.append("host=").append(host);
        builder.append(", port=").append(port);
        builder.append(", subset=").append(subset);
        builder.append('}');
        return builder.toString();
    }
}

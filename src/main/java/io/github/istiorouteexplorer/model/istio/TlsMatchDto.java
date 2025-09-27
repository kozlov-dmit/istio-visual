package io.github.istiorouteexplorer.model.istio;

import java.util.List;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * DTO capturing the match conditions for TLS routing rules in Istio.
 */
@NoArgsConstructor(force = true)
public class TlsMatchDto {

    private final List<String> destinationSubnets;
    private final List<String> gateways;
    private final Long port;
    private final List<String> sniHosts;

    public TlsMatchDto(List<String> destinationSubnets, List<String> gateways, Long port, List<String> sniHosts) {
        this.destinationSubnets = destinationSubnets;
        this.gateways = gateways;
        this.port = port;
        this.sniHosts = sniHosts;
    }

    public List<String> destinationSubnets() {
        return destinationSubnets;
    }

    public List<String> gateways() {
        return gateways;
    }

    public Long port() {
        return port;
    }

    public List<String> sniHosts() {
        return sniHosts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TlsMatchDto that)) {
            return false;
        }
        return Objects.equals(destinationSubnets, that.destinationSubnets) &&
            Objects.equals(gateways, that.gateways) &&
            Objects.equals(port, that.port) &&
            Objects.equals(sniHosts, that.sniHosts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(destinationSubnets, gateways, port, sniHosts);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("TlsMatchDto{");
        builder.append("destinationSubnets=").append(destinationSubnets);
        builder.append(", gateways=").append(gateways);
        builder.append(", port=").append(port);
        builder.append(", sniHosts=").append(sniHosts);
        builder.append('}');
        return builder.toString();
    }
}

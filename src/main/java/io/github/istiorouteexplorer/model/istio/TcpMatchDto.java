package io.github.istiorouteexplorer.model.istio;

import java.util.List;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * DTO capturing match conditions applied to Istio TCP routes.
 */
@NoArgsConstructor(force = true)
public class TcpMatchDto {

    private List<String> destinationSubnets;
    private List<String> gateways;
    private Long port;

    public TcpMatchDto(List<String> destinationSubnets, List<String> gateways, Long port) {
        this.destinationSubnets = destinationSubnets;
        this.gateways = gateways;
        this.port = port;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TcpMatchDto that)) {
            return false;
        }
        return Objects.equals(destinationSubnets, that.destinationSubnets) &&
            Objects.equals(gateways, that.gateways) &&
            Objects.equals(port, that.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(destinationSubnets, gateways, port);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("TcpMatchDto{");
        builder.append("destinationSubnets=").append(destinationSubnets);
        builder.append(", gateways=").append(gateways);
        builder.append(", port=").append(port);
        builder.append('}');
        return builder.toString();
    }
}

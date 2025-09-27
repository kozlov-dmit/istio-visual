package io.github.istiorouteexplorer.model.istio;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * DTO containing listener and TLS configuration defined inside an Istio Gateway.
 */
@NoArgsConstructor(force = true)
public class GatewaySpecDto {

    private final Map<String, String> selector;
    private final List<ServerDto> servers;

    public GatewaySpecDto(Map<String, String> selector, List<ServerDto> servers) {
        this.selector = selector;
        this.servers = servers;
    }

    public Map<String, String> selector() {
        return selector;
    }

    public List<ServerDto> servers() {
        return servers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GatewaySpecDto that)) {
            return false;
        }
        return Objects.equals(selector, that.selector) &&
            Objects.equals(servers, that.servers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(selector, servers);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("GatewaySpecDto{");
        builder.append("selector=").append(selector);
        builder.append(", servers=").append(servers);
        builder.append('}');
        return builder.toString();
    }
}

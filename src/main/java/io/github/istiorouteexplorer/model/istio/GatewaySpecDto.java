package io.github.istiorouteexplorer.model.istio;

import java.util.List;
import java.util.Map;

/**
 * Spec for Gateway
 * @param selector Map of labels
 * @param servers List of servers
 */
public record GatewaySpecDto(
        Map<String, String> selector,
        List<ServerDto> servers
) {
}

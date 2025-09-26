package io.github.istiorouteexplorer.model.istio;

import java.util.List;

/**
 * Server` describes the properties of the proxy on a given load balancer port
 *
 * @param bind bind address
 * @param defaultEndpoint default endpoint
 * @param hosts list of hosts
 * @param name name
 * @param port port
 * @param tls server tls settings
 */
public record ServerDto(
        String bind,
        String defaultEndpoint,
        List<String> hosts,
        String name,
        PortDto port,
        ServerTlsSettingsDto tls
) {
}

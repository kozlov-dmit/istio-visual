package io.github.istiorouteexplorer.model.istio;

import java.util.List;

/**
 * Specification of a VirtualService
 * @param hosts list of hostnames
 * @param http list of HTTP routes
 * @param tcp list of TCP routes
 * @param tls list of TLS routes
 */
public record VirtualServiceSpecDto(
        List<String> hosts,
        List<HttpRouteDto> http,
        List<TcpRouteDto> tcp,
        List<TlsRouteDto> tls
) {
}

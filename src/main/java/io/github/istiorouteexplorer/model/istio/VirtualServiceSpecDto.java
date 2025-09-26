package io.github.istiorouteexplorer.model.istio;

import java.util.List;
import java.util.Objects;

/**
 * Specification of a VirtualService
 * @param hosts list of hostnames
 * @param http list of HTTP routes
 * @param tcp list of TCP routes
 * @param tls list of TLS routes
 */

public final class VirtualServiceSpecDto {

    private final List<String> hosts;
    private final List<HttpRouteDto> http;
    private final List<TcpRouteDto> tcp;
    private final List<TlsRouteDto> tls;

    public VirtualServiceSpecDto(List<String> hosts, List<HttpRouteDto> http, List<TcpRouteDto> tcp, List<TlsRouteDto> tls) {
        this.hosts = hosts;
        this.http = http;
        this.tcp = tcp;
        this.tls = tls;
    }

    public List<String> hosts() {
        return hosts;
    }

    public List<HttpRouteDto> http() {
        return http;
    }

    public List<TcpRouteDto> tcp() {
        return tcp;
    }

    public List<TlsRouteDto> tls() {
        return tls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VirtualServiceSpecDto that)) {
            return false;
        }
        return Objects.equals(hosts, that.hosts) &&
            Objects.equals(http, that.http) &&
            Objects.equals(tcp, that.tcp) &&
            Objects.equals(tls, that.tls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hosts, http, tcp, tls);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("VirtualServiceSpecDto{");
        builder.append("hosts=").append(hosts);
        builder.append(", http=").append(http);
        builder.append(", tcp=").append(tcp);
        builder.append(", tls=").append(tls);
        builder.append('}');
        return builder.toString();
    }
}

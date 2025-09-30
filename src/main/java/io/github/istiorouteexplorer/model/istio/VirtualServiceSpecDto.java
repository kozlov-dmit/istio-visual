package io.github.istiorouteexplorer.model.istio;

import java.util.List;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * DTO holding the specification details of an Istio VirtualService.
 */
@NoArgsConstructor(force = true)
public class VirtualServiceSpecDto {

    private List<String> gateways;
    private List<String> hosts;
    private List<HttpRouteDto> http;
    private List<TcpRouteDto> tcp;
    private List<TlsRouteDto> tls;

    public VirtualServiceSpecDto(List<String> gateways, List<String> hosts, List<HttpRouteDto> http, List<TcpRouteDto> tcp, List<TlsRouteDto> tls) {
        this.gateways = gateways;
        this.hosts = hosts;
        this.http = http;
        this.tcp = tcp;
        this.tls = tls;
    }

    public List<String> gateways() {
        return gateways;
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
        return "VirtualServiceSpecDto{" + "gateways=" + gateways +
                "hosts=" + hosts +
                ", http=" + http +
                ", tcp=" + tcp +
                ", tls=" + tls +
                '}';
    }
}

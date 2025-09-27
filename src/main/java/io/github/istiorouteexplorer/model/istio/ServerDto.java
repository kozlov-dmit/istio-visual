package io.github.istiorouteexplorer.model.istio;

import java.util.List;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * DTO representing server configuration blocks defined on an Istio Gateway.
 */
@NoArgsConstructor(force = true)
public class ServerDto {

    private final String bind;
    private final String defaultEndpoint;
    private final List<String> hosts;
    private final String name;
    private final PortDto port;
    private final ServerTlsSettingsDto tls;

    public ServerDto(String bind, String defaultEndpoint, List<String> hosts, String name, PortDto port, ServerTlsSettingsDto tls) {
        this.bind = bind;
        this.defaultEndpoint = defaultEndpoint;
        this.hosts = hosts;
        this.name = name;
        this.port = port;
        this.tls = tls;
    }

    public String bind() {
        return bind;
    }

    public String defaultEndpoint() {
        return defaultEndpoint;
    }

    public List<String> hosts() {
        return hosts;
    }

    public String name() {
        return name;
    }

    public PortDto port() {
        return port;
    }

    public ServerTlsSettingsDto tls() {
        return tls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServerDto that)) {
            return false;
        }
        return Objects.equals(bind, that.bind) &&
            Objects.equals(defaultEndpoint, that.defaultEndpoint) &&
            Objects.equals(hosts, that.hosts) &&
            Objects.equals(name, that.name) &&
            Objects.equals(port, that.port) &&
            Objects.equals(tls, that.tls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bind, defaultEndpoint, hosts, name, port, tls);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("ServerDto{");
        builder.append("bind=").append(bind);
        builder.append(", defaultEndpoint=").append(defaultEndpoint);
        builder.append(", hosts=").append(hosts);
        builder.append(", name=").append(name);
        builder.append(", port=").append(port);
        builder.append(", tls=").append(tls);
        builder.append('}');
        return builder.toString();
    }
}

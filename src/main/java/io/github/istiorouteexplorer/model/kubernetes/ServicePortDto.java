package io.github.istiorouteexplorer.model.kubernetes;

import io.fabric8.kubernetes.api.model.IntOrString;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * DTO detailing port configuration for a Kubernetes Service.
 */
@NoArgsConstructor(force = true)
public class ServicePortDto {

    private final String appProtocol;
    private final String name;
    private final Integer nodePort;
    private final Integer port;
    private final String protocol;
    private final IntOrString targetPort;

    public ServicePortDto(String appProtocol, String name, Integer nodePort, Integer port, String protocol, IntOrString targetPort) {
        this.appProtocol = appProtocol;
        this.name = name;
        this.nodePort = nodePort;
        this.port = port;
        this.protocol = protocol;
        this.targetPort = targetPort;
    }

    public String appProtocol() {
        return appProtocol;
    }

    public String name() {
        return name;
    }

    public Integer nodePort() {
        return nodePort;
    }

    public Integer port() {
        return port;
    }

    public String protocol() {
        return protocol;
    }

    public IntOrString targetPort() {
        return targetPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServicePortDto that)) {
            return false;
        }
        return Objects.equals(appProtocol, that.appProtocol) &&
            Objects.equals(name, that.name) &&
            Objects.equals(nodePort, that.nodePort) &&
            Objects.equals(port, that.port) &&
            Objects.equals(protocol, that.protocol) &&
            Objects.equals(targetPort, that.targetPort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appProtocol, name, nodePort, port, protocol, targetPort);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("ServicePortDto{");
        builder.append("appProtocol=").append(appProtocol);
        builder.append(", name=").append(name);
        builder.append(", nodePort=").append(nodePort);
        builder.append(", port=").append(port);
        builder.append(", protocol=").append(protocol);
        builder.append(", targetPort=").append(targetPort);
        builder.append('}');
        return builder.toString();
    }
}

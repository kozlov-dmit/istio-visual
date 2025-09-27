package io.github.istiorouteexplorer.model.istio;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * DTO describing port configuration shared across Istio networking resources.
 */
@NoArgsConstructor(force = true)
public class PortDto {

    private final String name;
    private final Long number;
    private final String protocol;
    private final Long targetPort;

    public PortDto(String name, Long number, String protocol, Long targetPort) {
        this.name = name;
        this.number = number;
        this.protocol = protocol;
        this.targetPort = targetPort;
    }

    public String name() {
        return name;
    }

    public Long number() {
        return number;
    }

    public String protocol() {
        return protocol;
    }

    public Long targetPort() {
        return targetPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PortDto that)) {
            return false;
        }
        return Objects.equals(name, that.name) &&
            Objects.equals(number, that.number) &&
            Objects.equals(protocol, that.protocol) &&
            Objects.equals(targetPort, that.targetPort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, number, protocol, targetPort);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("PortDto{");
        builder.append("name=").append(name);
        builder.append(", number=").append(number);
        builder.append(", protocol=").append(protocol);
        builder.append(", targetPort=").append(targetPort);
        builder.append('}');
        return builder.toString();
    }
}

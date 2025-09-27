package io.github.istiorouteexplorer.model.istio;

import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * DTO capturing the target host, port, and subset information for an Istio route destination.
 */
@NoArgsConstructor(force = true)
public class DestinationDto {

    private String host;
    private Long port;
    private String subset;

    public DestinationDto(String host, Long port, String subset) {
        this.host = host;
        this.port = port;
        this.subset = subset;
    }

    public String host() {
        return host;
    }

    public Long port() {
        return port;
    }

    public String subset() {
        return subset;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(Long port) {
        this.port = port;
    }

    public void setSubset(String subset) {
        this.subset = subset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DestinationDto that)) {
            return false;
        }
        return Objects.equals(host, that.host) &&
            Objects.equals(port, that.port) &&
            Objects.equals(subset, that.subset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, subset);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("DestinationDto{");
        builder.append("host=").append(host);
        builder.append(", port=").append(port);
        builder.append(", subset=").append(subset);
        builder.append('}');
        return builder.toString();
    }
}

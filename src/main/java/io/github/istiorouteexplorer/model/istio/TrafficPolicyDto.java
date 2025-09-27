package io.github.istiorouteexplorer.model.istio;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * DTO aggregating traffic policy settings such as load balancing, connection pools, and TLS.
 */
@NoArgsConstructor(force = true)
public class TrafficPolicyDto {

    private ClientTlsSettingsDto tls;

    public TrafficPolicyDto(ClientTlsSettingsDto tls) {
        this.tls = tls;
    }

    public ClientTlsSettingsDto tls() {
        return tls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TrafficPolicyDto that)) {
            return false;
        }
        return Objects.equals(tls, that.tls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tls);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("TrafficPolicyDto{");
        builder.append("tls=").append(tls);
        builder.append('}');
        return builder.toString();
    }
}

package io.github.istiorouteexplorer.model.istio;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO aggregating traffic policy settings such as load balancing, connection pools, and TLS.
 */
@Data
@NoArgsConstructor(force = true)
public class TrafficPolicyDto {

    private ClientTlsSettingsDto tls;

    public List<String> merge(TrafficPolicyDto other) {
        List<String> errors = new ArrayList<>();
        if (tls == null) {
            tls = other.tls;
        }
        else if (other.tls != null && !tls.equals(other.tls)) {
            errors.addAll(tls.merge(other.tls));
        }
        return errors;
    }

}

package io.github.istiorouteexplorer.model.istio;

/**
 * Traffic policy
 *
 * @param tls TLS settings
 */
public record TrafficPolicyDto(
        ClientTlsSettingsDto tls
) {
}

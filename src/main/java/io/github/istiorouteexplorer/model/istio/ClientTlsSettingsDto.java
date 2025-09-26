package io.github.istiorouteexplorer.model.istio;

import java.util.List;

public record ClientTlsSettingsDto(
        String caCertificates,
        String caCrl,
        String clientCertificate,
        String credentialName,
        Boolean insecureSkipVerify,
        String privateKey,
        String sni,
        List<String> subjectAltNames,
        ClientTlsMode mode
) {
}

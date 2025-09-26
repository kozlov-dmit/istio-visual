package io.github.istiorouteexplorer.model.istio;

import java.util.List;

public record ServerTlsSettingsDto(
        String caCertificates,
        String caCrl,
        List<String> cipherSuites,
        String credentialName,
        Boolean httpsRedirect,
        ServerTlsMode mode,
        String privateKey,
        String serverCertificate,
        List<String> subjectAltNames
) {
}

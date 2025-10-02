package io.github.istiorouteexplorer.model.istio;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO encapsulating TLS options applied to servers within an Istio Gateway.
 */
@Data
@NoArgsConstructor(force = true)
public class ServerTlsSettingsDto {

    private String caCertificates;
    private String caCrl;
    private List<String> cipherSuites;
    private String credentialName;
    private Boolean httpsRedirect;
    private ServerTlsMode mode;
    private String privateKey;
    private String serverCertificate;
    private List<String> subjectAltNames;

}

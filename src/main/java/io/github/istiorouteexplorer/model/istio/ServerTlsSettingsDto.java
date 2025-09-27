package io.github.istiorouteexplorer.model.istio;

import java.util.List;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * DTO encapsulating TLS options applied to servers within an Istio Gateway.
 */
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

    public ServerTlsSettingsDto(String caCertificates, String caCrl, List<String> cipherSuites, String credentialName, Boolean httpsRedirect, ServerTlsMode mode, String privateKey, String serverCertificate, List<String> subjectAltNames) {
        this.caCertificates = caCertificates;
        this.caCrl = caCrl;
        this.cipherSuites = cipherSuites;
        this.credentialName = credentialName;
        this.httpsRedirect = httpsRedirect;
        this.mode = mode;
        this.privateKey = privateKey;
        this.serverCertificate = serverCertificate;
        this.subjectAltNames = subjectAltNames;
    }

    public String caCertificates() {
        return caCertificates;
    }

    public String caCrl() {
        return caCrl;
    }

    public List<String> cipherSuites() {
        return cipherSuites;
    }

    public String credentialName() {
        return credentialName;
    }

    public Boolean httpsRedirect() {
        return httpsRedirect;
    }

    public ServerTlsMode mode() {
        return mode;
    }

    public String privateKey() {
        return privateKey;
    }

    public String serverCertificate() {
        return serverCertificate;
    }

    public List<String> subjectAltNames() {
        return subjectAltNames;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServerTlsSettingsDto that)) {
            return false;
        }
        return Objects.equals(caCertificates, that.caCertificates) &&
            Objects.equals(caCrl, that.caCrl) &&
            Objects.equals(cipherSuites, that.cipherSuites) &&
            Objects.equals(credentialName, that.credentialName) &&
            Objects.equals(httpsRedirect, that.httpsRedirect) &&
            Objects.equals(mode, that.mode) &&
            Objects.equals(privateKey, that.privateKey) &&
            Objects.equals(serverCertificate, that.serverCertificate) &&
            Objects.equals(subjectAltNames, that.subjectAltNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caCertificates, caCrl, cipherSuites, credentialName, httpsRedirect, mode, privateKey, serverCertificate, subjectAltNames);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("ServerTlsSettingsDto{");
        builder.append("caCertificates=").append(caCertificates);
        builder.append(", caCrl=").append(caCrl);
        builder.append(", cipherSuites=").append(cipherSuites);
        builder.append(", credentialName=").append(credentialName);
        builder.append(", httpsRedirect=").append(httpsRedirect);
        builder.append(", mode=").append(mode);
        builder.append(", privateKey=").append(privateKey);
        builder.append(", serverCertificate=").append(serverCertificate);
        builder.append(", subjectAltNames=").append(subjectAltNames);
        builder.append('}');
        return builder.toString();
    }
}

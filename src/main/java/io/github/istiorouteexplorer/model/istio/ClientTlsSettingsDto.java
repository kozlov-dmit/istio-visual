package io.github.istiorouteexplorer.model.istio;

import java.util.List;
import java.util.Objects;

public final class ClientTlsSettingsDto {

    private final String caCertificates;
    private final String caCrl;
    private final String clientCertificate;
    private final String credentialName;
    private final Boolean insecureSkipVerify;
    private final String privateKey;
    private final String sni;
    private final List<String> subjectAltNames;
    private final ClientTlsMode mode;

    public ClientTlsSettingsDto(String caCertificates, String caCrl, String clientCertificate, String credentialName, Boolean insecureSkipVerify, String privateKey, String sni, List<String> subjectAltNames, ClientTlsMode mode) {
        this.caCertificates = caCertificates;
        this.caCrl = caCrl;
        this.clientCertificate = clientCertificate;
        this.credentialName = credentialName;
        this.insecureSkipVerify = insecureSkipVerify;
        this.privateKey = privateKey;
        this.sni = sni;
        this.subjectAltNames = subjectAltNames;
        this.mode = mode;
    }

    public String caCertificates() {
        return caCertificates;
    }

    public String caCrl() {
        return caCrl;
    }

    public String clientCertificate() {
        return clientCertificate;
    }

    public String credentialName() {
        return credentialName;
    }

    public Boolean insecureSkipVerify() {
        return insecureSkipVerify;
    }

    public String privateKey() {
        return privateKey;
    }

    public String sni() {
        return sni;
    }

    public List<String> subjectAltNames() {
        return subjectAltNames;
    }

    public ClientTlsMode mode() {
        return mode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClientTlsSettingsDto that)) {
            return false;
        }
        return Objects.equals(caCertificates, that.caCertificates) &&
            Objects.equals(caCrl, that.caCrl) &&
            Objects.equals(clientCertificate, that.clientCertificate) &&
            Objects.equals(credentialName, that.credentialName) &&
            Objects.equals(insecureSkipVerify, that.insecureSkipVerify) &&
            Objects.equals(privateKey, that.privateKey) &&
            Objects.equals(sni, that.sni) &&
            Objects.equals(subjectAltNames, that.subjectAltNames) &&
            Objects.equals(mode, that.mode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caCertificates, caCrl, clientCertificate, credentialName, insecureSkipVerify, privateKey, sni, subjectAltNames, mode);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("ClientTlsSettingsDto{");
        builder.append("caCertificates=").append(caCertificates);
        builder.append(", caCrl=").append(caCrl);
        builder.append(", clientCertificate=").append(clientCertificate);
        builder.append(", credentialName=").append(credentialName);
        builder.append(", insecureSkipVerify=").append(insecureSkipVerify);
        builder.append(", privateKey=").append(privateKey);
        builder.append(", sni=").append(sni);
        builder.append(", subjectAltNames=").append(subjectAltNames);
        builder.append(", mode=").append(mode);
        builder.append('}');
        return builder.toString();
    }
}

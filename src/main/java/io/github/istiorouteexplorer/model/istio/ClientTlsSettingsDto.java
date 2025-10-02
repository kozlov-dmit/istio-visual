package io.github.istiorouteexplorer.model.istio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO describing the Istio client TLS configuration extracted from cluster resources.
 */
@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class ClientTlsSettingsDto {

    private String caCertificates;
    private String caCrl;
    private String clientCertificate;
    private String credentialName;
    private Boolean insecureSkipVerify;
    private String privateKey;
    private String sni;
    private List<String> subjectAltNames;
    private ClientTlsMode mode;

    public List<String> merge(ClientTlsSettingsDto other) {
        List<String> errors = new ArrayList<>();

        if (caCertificates == null && other.getCaCertificates() != null) {
            caCertificates = other.getCaCertificates();
        }
        else if (caCertificates != null && other.getCaCertificates() != null && !caCertificates.equals(other.getCaCertificates())) {
            errors.add("caCertificates already set, but found another value");
        }

        if (caCrl == null && other.getCaCrl() != null) {
            caCrl = other.getCaCrl();
        }
        else if (caCrl != null && other.getCaCrl() != null && !caCrl.equals(other.getCaCrl())) {
            errors.add("caCrl already set, but found another value");
        }
        if (clientCertificate == null && other.getClientCertificate() != null) {
            clientCertificate = other.getClientCertificate();
        }
        else if (clientCertificate != null && other.getClientCertificate() != null && !clientCertificate.equals(other.getClientCertificate())) {
            errors.add("clientCertificate already set, but found another value");
        }
        if (credentialName == null && other.getCredentialName() != null) {
            credentialName = other.getCredentialName();
        }
        else if (credentialName != null && other.getCredentialName() != null && !credentialName.equals(other.getCredentialName())) {
            errors.add("credentialName already set, but found another value");
        }
        if (insecureSkipVerify == null && other.getInsecureSkipVerify() != null) {
            insecureSkipVerify = other.getInsecureSkipVerify();
        }
        else if (insecureSkipVerify != null && other.getInsecureSkipVerify() != null && !insecureSkipVerify.equals(other.getInsecureSkipVerify())) {
            errors.add("insecureSkipVerify already set, but found another value");
        }
        if (privateKey == null && other.getPrivateKey() != null) {
            privateKey = other.getPrivateKey();
        }
        else if (privateKey != null && other.getPrivateKey() != null && !privateKey.equals(other.getPrivateKey())) {
            errors.add("privateKey already set, but found another value");
        }
        if (sni == null && other.getSni() != null) {
            sni = other.getSni();
        }
        else if (sni != null && other.getSni() != null && !sni.equals(other.getSni())) {
            errors.add("sni already set, but found another value");
        }
        if (subjectAltNames == null && other.getSubjectAltNames() != null) {
            subjectAltNames = other.getSubjectAltNames();
        }
        else if (subjectAltNames != null && other.getSubjectAltNames() != null) {
            subjectAltNames.addAll(other.getSubjectAltNames());
        }
        if (mode == null && other.getMode() != null) {
            mode = other.getMode();
        }
        else if (mode != null && other.getMode() != null && !mode.equals(other.getMode())) {
            errors.add("mode already set as" + mode + ", but found another value " + other.getMode());
        }

        return errors;
    }

}

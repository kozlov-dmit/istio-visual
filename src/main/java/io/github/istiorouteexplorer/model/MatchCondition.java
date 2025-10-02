package io.github.istiorouteexplorer.model;

import io.github.istiorouteexplorer.model.istio.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Conditions to find route: uri prefix/regex, headers, sni etc.
 */
@Data
@AllArgsConstructor
public class MatchCondition {
    private String field;
    private String kind;
    private String value;

    public static List<MatchCondition> fromMatch(IstioMatchRequestDto matchRequest) {
        return switch (matchRequest) {
            case HttpMatchRequestDto httpMatchRequestDto -> fromHttp(httpMatchRequestDto);
            case TcpMatchRequestDto tcpMatchRequestDto -> fromTcp(tcpMatchRequestDto);
            case TlsMatchRequestDto tlsMatchRequestDto -> fromTls(tlsMatchRequestDto);
            default -> throw new IllegalArgumentException("Unsupported match request type: " + matchRequest.getClass().getName());
        };
    }

    public static List<MatchCondition> fromHttp(HttpMatchRequestDto httpMatchRequest) {
        if (httpMatchRequest == null) {
            return List.of();
        }
        List<MatchCondition> matchConditions = new ArrayList<>();
        if (httpMatchRequest.getUri() != null) {
            matchConditions.add(createMatchCondition("uri", httpMatchRequest.getUri()));
        }
        if (httpMatchRequest.getHeaders() != null) {
            httpMatchRequest.getHeaders().forEach((key, value) -> matchConditions.add(createMatchCondition("header=" + key, value)));
        }
        // authority = header Host
        if (httpMatchRequest.getAuthority() != null) {
            matchConditions.add(createMatchCondition("header=Host", httpMatchRequest.getAuthority()));
        }
        if (httpMatchRequest.getPort() != null) {
            matchConditions.add(new MatchCondition("port", "exact", httpMatchRequest.getPort().toString()));
        }
        return matchConditions;
    }

    public static List<MatchCondition> fromTcp(TcpMatchRequestDto tcpMatchRequest) {
        if (tcpMatchRequest == null) {
            return List.of();
        }
        List<MatchCondition> matchConditions = new ArrayList<>();
        if (tcpMatchRequest.getDestinationSubnets() != null && !tcpMatchRequest.getDestinationSubnets().isEmpty()) {
            tcpMatchRequest.getDestinationSubnets().forEach(subnet -> matchConditions.add(new MatchCondition("destinationSubnet", "exact", subnet)));
        }
        if (tcpMatchRequest.getGateways() != null && !tcpMatchRequest.getGateways().isEmpty()) {
            tcpMatchRequest.getGateways().forEach(gateway -> matchConditions.add(new MatchCondition("gateway", "exact", gateway)));
        }
        if (tcpMatchRequest.getPort() != null) {
            matchConditions.add(new MatchCondition("port", "exact", tcpMatchRequest.getPort().toString()));
        }

        return matchConditions;
    }

    public static List<MatchCondition> fromTls(TlsMatchRequestDto tlsMatchRequest) {
        if (tlsMatchRequest == null) {
            return List.of();
        }
        List<MatchCondition> matchConditions = new ArrayList<>();
        if (tlsMatchRequest.getDestinationSubnets() != null && !tlsMatchRequest.getDestinationSubnets().isEmpty()) {
            tlsMatchRequest.getDestinationSubnets().forEach(subnet -> matchConditions.add(new MatchCondition("destinationSubnet", "exact", subnet)));
        }
        if (tlsMatchRequest.getGateways() != null && !tlsMatchRequest.getGateways().isEmpty()) {
            tlsMatchRequest.getGateways().forEach(gateway -> matchConditions.add(new MatchCondition("gateway", "exact", gateway)));
        }
        if (tlsMatchRequest.getPort() != null) {
            matchConditions.add(new MatchCondition("port", "exact", tlsMatchRequest.getPort().toString()));
        }
        if (tlsMatchRequest.getSniHosts() != null) {
            tlsMatchRequest.getSniHosts().forEach(sniHost -> matchConditions.add(new MatchCondition("sni", "exact", sniHost)));
        }

        return matchConditions;
    }

    private static MatchCondition createMatchCondition(String field, StringMatchDto stringMatch) {
        if (stringMatch.isExactMatch()) {
            return new MatchCondition(field, "exact", stringMatch.getExact());
        } else if (stringMatch.isRegexMatch()) {
            return new MatchCondition(field, "regex", stringMatch.getRegex());
        } else if (stringMatch.isPrefixMatch()) {
            return new MatchCondition(field, "prefix", stringMatch.getPrefix());
        }
        return null;
    }
}

package io.github.istiorouteexplorer.model;

import io.github.istiorouteexplorer.model.istio.HttpMatchRequestDto;
import io.github.istiorouteexplorer.model.istio.StringMatchDto;
import io.github.istiorouteexplorer.model.istio.TcpMatchRequestDto;
import io.github.istiorouteexplorer.model.istio.TlsMatchRequestDto;
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
        if (tcpMatchRequest.destinationSubnets() != null && !tcpMatchRequest.destinationSubnets().isEmpty()) {
            tcpMatchRequest.destinationSubnets().forEach(subnet -> matchConditions.add(new MatchCondition("destinationSubnet", "exact", subnet)));
        }
        if (tcpMatchRequest.gateways() != null && !tcpMatchRequest.gateways().isEmpty()) {
            tcpMatchRequest.gateways().forEach(gateway -> matchConditions.add(new MatchCondition("gateway", "exact", gateway)));
        }
        if (tcpMatchRequest.port() != null) {
            matchConditions.add(new MatchCondition("port", "exact", tcpMatchRequest.port().toString()));
        }

        return matchConditions;
    }

    public static List<MatchCondition> fromTls(TlsMatchRequestDto tlsMatchRequest) {
        if (tlsMatchRequest == null) {
            return List.of();
        }
        List<MatchCondition> matchConditions = new ArrayList<>();
        if (tlsMatchRequest.destinationSubnets() != null && !tlsMatchRequest.destinationSubnets().isEmpty()) {
            tlsMatchRequest.destinationSubnets().forEach(subnet -> matchConditions.add(new MatchCondition("destinationSubnet", "exact", subnet)));
        }
        if (tlsMatchRequest.gateways() != null && !tlsMatchRequest.gateways().isEmpty()) {
            tlsMatchRequest.gateways().forEach(gateway -> matchConditions.add(new MatchCondition("gateway", "exact", gateway)));
        }
        if (tlsMatchRequest.port() != null) {
            matchConditions.add(new MatchCondition("port", "exact", tlsMatchRequest.port().toString()));
        }
        if (tlsMatchRequest.sniHosts() != null) {
            tlsMatchRequest.sniHosts().forEach(sniHost -> matchConditions.add(new MatchCondition("sni", "exact", sniHost)));
        }

        return matchConditions;
    }

    private static MatchCondition createMatchCondition(String field, StringMatchDto stringMatch) {
        if (stringMatch.isExactMatch()) {
            return new MatchCondition(field, "exact", stringMatch.getExact());
        } else if (stringMatch.isRegexMatch()) {
            return new MatchCondition(field, "regex", stringMatch.getRegex());
        } else {
            return new MatchCondition(field, "prefix", stringMatch.getPrefix());
        }
    }
}

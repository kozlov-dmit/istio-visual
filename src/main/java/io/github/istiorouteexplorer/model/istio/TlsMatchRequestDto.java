package io.github.istiorouteexplorer.model.istio;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO capturing the match conditions for TLS routing rules in Istio.
 */
@Data
@NoArgsConstructor(force = true)
public class TlsMatchRequestDto implements IstioMatchRequestDto {

    private List<String> destinationSubnets;
    private List<String> gateways;
    private Long port;
    private List<String> sniHosts;

}

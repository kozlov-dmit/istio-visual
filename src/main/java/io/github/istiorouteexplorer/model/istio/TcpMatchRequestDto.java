package io.github.istiorouteexplorer.model.istio;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO capturing match conditions applied to Istio TCP routes.
 */
@Data
@NoArgsConstructor(force = true)
public class TcpMatchRequestDto implements IstioMatchRequestDto {

    private List<String> destinationSubnets;
    private List<String> gateways;
    private Long port;

}

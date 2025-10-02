package io.github.istiorouteexplorer.model.istio;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO containing listener and TLS configuration defined inside an Istio Gateway.
 */
@Data
@NoArgsConstructor(force = true)
public class GatewaySpecDto {

    private Map<String, String> selector;
    private List<ServerDto> servers;

}

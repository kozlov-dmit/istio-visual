package io.github.istiorouteexplorer.model.istio;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO holding the specification details of an Istio VirtualService.
 */
@Data
@NoArgsConstructor(force = true)
public class VirtualServiceSpecDto {

    private List<String> gateways;
    private List<String> hosts;
    private List<HttpRouteDto> http;
    private List<TcpRouteDto> tcp;
    private List<TlsRouteDto> tls;

}

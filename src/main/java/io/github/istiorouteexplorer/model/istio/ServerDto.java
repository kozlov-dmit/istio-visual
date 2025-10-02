package io.github.istiorouteexplorer.model.istio;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing server configuration blocks defined on an Istio Gateway.
 */
@Data
@NoArgsConstructor(force = true)
public class ServerDto {

    private String bind;
    private String defaultEndpoint;
    private List<String> hosts;
    private String name;
    private PortDto port;
    private ServerTlsSettingsDto tls;

}

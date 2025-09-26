package io.github.istiorouteexplorer.model.istio;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Port
 * @param name name
 * @param number number
 * @param protocol protocol
 * @param targetPort targetPort
 */
public record PortDto(
        String name,
        Long number,
        String protocol,
        Long targetPort
) {
}

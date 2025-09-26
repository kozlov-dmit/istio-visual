package io.github.istiorouteexplorer.model.kubernetes;

import io.fabric8.kubernetes.api.model.IntOrString;

/**
 * ServicePort contains information on service's port.
 *
 * @param appProtocol The application protocol for this port. This is used as a hint for implementations to offer richer behavior for protocols that they understand
 * @param name The name of this port within the service. This must be a DNS_LABEL.
 * @param nodePort The port on each node on which this service is exposed when type is NodePort or LoadBalancer.
 * @param port The port that will be exposed by this service.
 * @param protocol The IP protocol for this port. Supports "TCP", "UDP", and "SCTP". Default is TCP
 * @param targetPort targetPort
 */
public record ServicePortDto(
        String appProtocol,
        String name,
        Integer nodePort,
        Integer port,
        String protocol,
        IntOrString targetPort
) {
}

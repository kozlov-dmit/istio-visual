package io.github.istiorouteexplorer.model.envoy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record EnvoyPodSummary(
        String name,
        String namespace,
        String phase,
        String podIp,
        String hostIp,
        String nodeName,
        String serviceAccountName,
        Instant creationTimestamp,
        Map<String, String> labels,
        Map<String, String> annotations,
        List<EnvoyContainerStatus> containers
) {

    public record EnvoyContainerStatus(String name, boolean ready, Integer restartCount, String image) {
    }
}

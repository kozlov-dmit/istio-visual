package io.github.istiorouteexplorer.model.envoy;

import java.util.List;

public record EnvoyConfigResponse(
        EnvoyPodSummary pod,
        List<EnvoyConfigSection> sections,
        List<String> warnings
) {
}

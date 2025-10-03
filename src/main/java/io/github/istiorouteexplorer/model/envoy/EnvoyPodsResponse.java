package io.github.istiorouteexplorer.model.envoy;

import java.util.List;

public record EnvoyPodsResponse(List<EnvoyPodSummary> pods) {
}

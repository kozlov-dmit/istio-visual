package io.github.istiorouteexplorer.model.kubernetes;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO capturing status information for a Kubernetes Pod.
 */
@Data
@NoArgsConstructor(force = true)
public class PodStatusDto {

    private String phase;

}

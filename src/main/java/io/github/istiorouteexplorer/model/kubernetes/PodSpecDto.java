package io.github.istiorouteexplorer.model.kubernetes;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO providing specification details for a Kubernetes Pod used when building topology.
 */
@Data
@NoArgsConstructor(force = true)
public class PodSpecDto {

    private List<ContainerDto> containers;

}

package io.github.istiorouteexplorer.model.kubernetes;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO capturing core metadata for a Kubernetes container participating in traffic graphs.
 */
@Data
@NoArgsConstructor(force = true)
public class ContainerDto {

    private String name;
    private String image;

}

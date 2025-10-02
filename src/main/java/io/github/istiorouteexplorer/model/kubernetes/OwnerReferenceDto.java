package io.github.istiorouteexplorer.model.kubernetes;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reference to resource, that is owned by this resource.
 */
@Data
@NoArgsConstructor
public class OwnerReferenceDto {

    private String kind;
    private String name;

}

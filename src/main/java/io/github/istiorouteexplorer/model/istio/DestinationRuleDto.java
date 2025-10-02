package io.github.istiorouteexplorer.model.istio;

import io.github.istiorouteexplorer.model.kubernetes.ObjectMetadataDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing an Istio DestinationRule resource with associated metadata and spec.
 */
@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class DestinationRuleDto {

    private ObjectMetadataDto metadata;
    private DestinationRuleSpecDto spec;

}

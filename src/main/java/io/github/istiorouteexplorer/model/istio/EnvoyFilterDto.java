package io.github.istiorouteexplorer.model.istio;

import io.github.istiorouteexplorer.model.kubernetes.ObjectMetadataDto;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EnvoyFilterDto {

    private ObjectMetadataDto metadata;
    private EnvoyFilterSpecDto spec;

}

package io.github.istiorouteexplorer.model.istio;

import io.github.istiorouteexplorer.model.kubernetes.ObjectMetadataDto;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class EnvoyFilterDto {

    private ObjectMetadataDto metadata;
    private EnvoyFilterSpecDto spec;

    public ObjectMetadataDto getMetadata() {
        return metadata;
    }

    public void setMetadata(ObjectMetadataDto metadata) {
        this.metadata = metadata;
    }

    public EnvoyFilterSpecDto getSpec() {
        return spec;
    }

    public void setSpec(EnvoyFilterSpecDto spec) {
        this.spec = spec;
    }

}

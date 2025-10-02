package io.github.istiorouteexplorer.model.kubernetes;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DeploymentDto {

    private ObjectMetadataDto metadata;
    private DeploymentSpecDto spec;
    private DeploymentStatusDto status;

}

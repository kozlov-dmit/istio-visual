package io.github.istiorouteexplorer.model;

import io.github.istiorouteexplorer.model.kubernetes.ObjectMetadataDto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class KubernetesResourceDto {

    @Getter
    private final String name;
    @Getter
    private final ObjectMetadataDto metadata;

}

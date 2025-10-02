package io.github.istiorouteexplorer.model.kubernetes;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DTO mirroring Kubernetes object metadata such as name, namespace, labels, and annotations.
 */
@Data
@NoArgsConstructor(force = true)
public class ObjectMetadataDto {

    private String name;
    private String namespace;
    private Map<String, String> labels;
    private Map<String, String> annotations;
    private List<OwnerReferenceDto> ownerReferences = new ArrayList<>();

    public ObjectMetadataDto(String name, String namespace, Map<String, String> labels, Map<String, String> annotations) {
        this.name = name;
        this.namespace = namespace;
        this.labels = labels;
        this.annotations = annotations;
    }

    public Map<String, Object> toMap() {
        return Map.of(
                "name", name,
                "namespace", namespace,
                "labels", labels,
                "annotations", annotations
        );
    }

}

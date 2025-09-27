package io.github.istiorouteexplorer.model.kubernetes;

import java.util.Map;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * DTO mirroring Kubernetes object metadata such as name, namespace, labels, and annotations.
 */
@NoArgsConstructor(force = true)
public class ObjectMetadataDto {

    private final String name;
    private final String namespace;
    private final Map<String, String> labels;
    private final Map<String, String> annotations;

    public ObjectMetadataDto(String name, String namespace, Map<String, String> labels, Map<String, String> annotations) {
        this.name = name;
        this.namespace = namespace;
        this.labels = labels;
        this.annotations = annotations;
    }

    public String name() {
        return name;
    }

    public String namespace() {
        return namespace;
    }

    public Map<String, String> labels() {
        return labels;
    }

    public Map<String, String> annotations() {
        return annotations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ObjectMetadataDto that)) {
            return false;
        }
        return Objects.equals(name, that.name) &&
            Objects.equals(namespace, that.namespace) &&
            Objects.equals(labels, that.labels) &&
            Objects.equals(annotations, that.annotations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, namespace, labels, annotations);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("ObjectMetadataDto{");
        builder.append("name=").append(name);
        builder.append(", namespace=").append(namespace);
        builder.append(", labels=").append(labels);
        builder.append(", annotations=").append(annotations);
        builder.append('}');
        return builder.toString();
    }
}

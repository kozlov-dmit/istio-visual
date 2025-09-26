package io.github.istiorouteexplorer.model.kubernetes;
import java.util.Objects;

/**
 * Container
 * @param name container name
 * @param image image
 */

public final class ContainerDto {

    private final String name;
    private final String image;

    public ContainerDto(String name, String image) {
        this.name = name;
        this.image = image;
    }

    public String name() {
        return name;
    }

    public String image() {
        return image;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ContainerDto that)) {
            return false;
        }
        return Objects.equals(name, that.name) &&
            Objects.equals(image, that.image);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, image);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("ContainerDto{");
        builder.append("name=").append(name);
        builder.append(", image=").append(image);
        builder.append('}');
        return builder.toString();
    }
}

package io.github.istiorouteexplorer.model;

import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * Describes an endpoint participating in a rendered traffic route (service, gateway, mesh or external).
 */
@NoArgsConstructor(force = true)
public class RouteNodeView {

    private String id;
    private String label;
    private String subtitle;
    private String type;

    public RouteNodeView(String id, String label, String subtitle, String type) {
        this.id = id;
        this.label = label;
        this.subtitle = subtitle;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String id() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String label() {
        return label;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String subtitle() {
        return subtitle;
    }

    public String getType() {
        return type;
    }

    public String type() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RouteNodeView that)) {
            return false;
        }
        return Objects.equals(id, that.id)
                && Objects.equals(label, that.label)
                && Objects.equals(subtitle, that.subtitle)
                && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, subtitle, type);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("RouteNodeView{");
        builder.append("id='").append(id).append('\'');
        builder.append(", label='").append(label).append('\'');
        builder.append(", subtitle='").append(subtitle).append('\'');
        builder.append(", type='").append(type).append('\'');
        builder.append('}');
        return builder.toString();
    }
}

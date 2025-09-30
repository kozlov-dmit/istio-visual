package io.github.istiorouteexplorer.model;

import java.util.List;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * Represents a rendered route between two endpoints annotated with rule summaries.
 */
@NoArgsConstructor(force = true)
public class RouteEdgeView {

    private String id;
    private RouteNodeView source;
    private RouteNodeView target;
    private List<String> rules;

    public RouteEdgeView(String id, RouteNodeView source, RouteNodeView target, List<String> rules) {
        this.id = id;
        this.source = source;
        this.target = target;
        this.rules = rules == null ? List.of() : List.copyOf(rules);
    }

    public String getId() {
        return id;
    }

    public String id() {
        return id;
    }

    public RouteNodeView getSource() {
        return source;
    }

    public RouteNodeView source() {
        return source;
    }

    public RouteNodeView getTarget() {
        return target;
    }

    public RouteNodeView target() {
        return target;
    }

    public List<String> getRules() {
        return rules;
    }

    public List<String> rules() {
        return rules;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RouteEdgeView that)) {
            return false;
        }
        return Objects.equals(id, that.id)
                && Objects.equals(source, that.source)
                && Objects.equals(target, that.target)
                && Objects.equals(rules, that.rules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, source, target, rules);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("RouteEdgeView{");
        builder.append("id='").append(id).append('\'');
        builder.append(", source=").append(source);
        builder.append(", target=").append(target);
        builder.append(", rules=").append(rules);
        builder.append('}');
        return builder.toString();
    }
}

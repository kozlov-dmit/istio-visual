package io.github.istiorouteexplorer.model;

import java.util.List;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * Groups related route edges under a single VirtualService to simplify UI rendering.
 */
@NoArgsConstructor(force = true)
public class RouteGroupView {

    private String id;
    private String label;
    private String namespace;
    private List<String> hosts;
    private List<String> gateways;
    private List<RouteEdgeView> edges;

    public RouteGroupView(String id, String label, String namespace,
                          List<String> hosts, List<String> gateways, List<RouteEdgeView> edges) {
        this.id = id;
        this.label = label;
        this.namespace = namespace;
        this.hosts = hosts == null ? List.of() : List.copyOf(hosts);
        this.gateways = gateways == null ? List.of() : List.copyOf(gateways);
        this.edges = edges == null ? List.of() : List.copyOf(edges);
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

    public String getNamespace() {
        return namespace;
    }

    public String namespace() {
        return namespace;
    }

    public List<String> getHosts() {
        return hosts;
    }

    public List<String> hosts() {
        return hosts;
    }

    public List<String> getGateways() {
        return gateways;
    }

    public List<String> gateways() {
        return gateways;
    }

    public List<RouteEdgeView> getEdges() {
        return edges;
    }

    public List<RouteEdgeView> edges() {
        return edges;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RouteGroupView that)) {
            return false;
        }
        return Objects.equals(id, that.id)
                && Objects.equals(label, that.label)
                && Objects.equals(namespace, that.namespace)
                && Objects.equals(hosts, that.hosts)
                && Objects.equals(gateways, that.gateways)
                && Objects.equals(edges, that.edges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, namespace, hosts, gateways, edges);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("RouteGroupView{");
        builder.append("id='").append(id).append('\'');
        builder.append(", label='").append(label).append('\'');
        builder.append(", namespace='").append(namespace).append('\'');
        builder.append(", hosts=").append(hosts);
        builder.append(", gateways=").append(gateways);
        builder.append(", edges=").append(edges);
        builder.append('}');
        return builder.toString();
    }
}

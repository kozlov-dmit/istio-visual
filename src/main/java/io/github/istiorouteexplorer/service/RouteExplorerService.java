package io.github.istiorouteexplorer.service;

import io.github.istiorouteexplorer.config.AppProperties;
import io.github.istiorouteexplorer.graph.GraphBuilder;
import io.github.istiorouteexplorer.graph.TopologyBuilder;
import io.github.istiorouteexplorer.kube.IstioResourceLoader;
import io.github.istiorouteexplorer.model.GraphResponse;
import io.github.istiorouteexplorer.model.ResourceCollection;
import io.github.istiorouteexplorer.model.TopologyGraph;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RouteExplorerService {

    private final AppProperties properties;
    private final IstioResourceLoader loader;
    private final GraphBuilder graphBuilder;
    private final TopologyBuilder topologyBuilder;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public RouteExplorerService(AppProperties properties, IstioResourceLoader loader, GraphBuilder graphBuilder, TopologyBuilder topologyBuilder) {
        this.properties = properties;
        this.loader = loader;
        this.graphBuilder = graphBuilder;
        this.topologyBuilder = topologyBuilder;
    }

    public TopologyGraph buildTopologyGraph(String namespace) {
        String ns = (namespace == null || namespace.isBlank()) ? properties.getNamespace() : namespace;
        try {
            ResourceCollection resourceCollection = loader.load(ns, properties.getExtraNamespaces());
            return topologyBuilder.build(resourceCollection);
        }
        catch (IOException e) {
            throw new RouteExplorerException("Failed to load resources for namespace " + ns, e);
        }
    }

    public GraphResponse buildGraph(String namespace) {
        String ns = (namespace == null || namespace.isBlank()) ? properties.getNamespace() : namespace;
        Duration ttl = properties.getCacheTtl();
        if (!isZeroOrNegative(ttl)) {
            GraphResponse cached = lookupCache(ns);
            if (cached != null) {
                return cached;
            }
        }
        try {
            ResourceCollection collection = loader.load(ns, properties.getExtraNamespaces());
            GraphResponse response = graphBuilder.build(collection);
            if (!isZeroOrNegative(ttl)) {
                cache.put(ns, new CacheEntry(response, Instant.now().plus(ttl)));
            }
            return response;
        } catch (IOException e) {
            throw new RouteExplorerException("Failed to load resources for namespace " + ns, e);
        }
    }

    private GraphResponse lookupCache(String namespace) {
        CacheEntry entry = cache.get(namespace);
        if (entry == null) {
            return null;
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            cache.remove(namespace, entry);
            return null;
        }
        return entry.response();
    }

    private boolean isZeroOrNegative(Duration duration) {
        return duration == null || duration.isZero() || duration.isNegative();
    }

    private record CacheEntry(GraphResponse response, Instant expiresAt) {
    }
}

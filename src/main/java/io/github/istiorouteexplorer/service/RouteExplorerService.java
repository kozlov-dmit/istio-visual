package io.github.istiorouteexplorer.service;

import io.github.istiorouteexplorer.config.AppProperties;
import io.github.istiorouteexplorer.kube.IstioResourceLoader;
import io.github.istiorouteexplorer.model.ResourceCollection;
import io.github.istiorouteexplorer.model.RoutesResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@AllArgsConstructor
public class RouteExplorerService {

    private final AppProperties properties;
    private final IstioResourceLoader loader;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public RoutesResponse buildRoutes(String namespace) {
        String ns = (namespace == null || namespace.isBlank()) ? properties.getNamespace() : namespace;
        Duration ttl = properties.getCacheTtl();
        if (isPositive(ttl)) {
            RoutesResponse cached = lookupCache(ns);
            if (cached != null) {
                return cached;
            }
        }
        try {
            ResourceCollection collection = loader.load(ns, properties.getExtraNamespaces());
            RouteExplorer routeExplorer = new RouteExplorer(collection);
            RoutesResponse response = new RoutesResponse(routeExplorer.buildRoutes());
            if (isPositive(ttl)) {
                cache.put(ns, new CacheEntry(response, Instant.now().plus(ttl)));
            }
            return response;
        } catch (IOException e) {
            throw new RouteExplorerException("Failed to load resources for namespace " + ns, e);
        }
    }

    private RoutesResponse lookupCache(String namespace) {
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

    private boolean isPositive(Duration duration) {
        return duration == null || duration.isPositive();
    }

    private record CacheEntry(RoutesResponse response, Instant expiresAt) {
    }
}

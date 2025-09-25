# Istio Route Explorer (Java Edition)

Istio Route Explorer is a Spring Boot 3 / Java 21 service that reads Kubernetes and Istio resources in a namespace, builds a routing graph, and serves it through both a JSON API and an interactive web UI. Supply a kubeconfig path when running locally to connect to your cluster. The UI now renders container-level packet routes: application containers, their Istio sidecars, and external services are visualised so you can follow how traffic moves inside the namespace and to outbound dependencies.

## Build & Run Locally

Requirements:

- Java 21 (e.g. Temurin 21)
- Maven 3.9+

```bash
# From the repository root
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--app.kube-config=/path/to/kubeconfig --app.namespace=default"
```

Alternatively build a runnable jar:

```bash
mvn package
java -jar target/istio-route-explorer-0.1.0-SNAPSHOT.jar \
  --app.kube-config=/path/to/kubeconfig \
  --app.namespace=istio-system \
  --app.extra-namespaces=shared-config
```

Key arguments / environment variables:

Property | Description | Default
-------- | ----------- | -------
`app.kube-config` | Path to a kubeconfig file. Leave empty to use in-cluster configuration. | *(empty)*
`app.namespace` | Primary namespace to inspect when clients omit `?namespace=` | `default`
`app.extra-namespaces` | Comma separated list of additional namespaces to include for shared destination rules | *(empty)*
`app.cache-ttl` | Cache TTL (e.g. `15s`). Set `0s` to disable caching. | `15s`
`app.request-timeout` | Timeout for Kubernetes API calls (e.g. `10s`) | `10s`
`app.skip-tls-verify` | Set to `true` to skip TLS verification | `false`

After the service starts, open `http://localhost:8080/` and enter a namespace in the top bar (defaults to the configured `app.namespace`). Each node exposes its associated resources section so you can inspect VirtualServices, DestinationRules, Kubernetes Services, and ServiceEntries directly from the UI. The JSON graph is available at `GET /api/graph?namespace=<name>`.

## Docker

Build and run the container image:

```bash
docker build -t istio-route-explorer:latest .

docker run --rm -p 8080:8080 \
  -e APP_NAMESPACE=default \
  -e APP_KUBE_CONFIG=/kube/config \
  -v $HOME/.kube/config:/kube/config:ro \
  istio-route-explorer:latest \
  --app.kube-config=/kube/config --app.namespace=default
```

You can pass any Spring Boot property as an argument (e.g. `--app.cache-ttl=0s`) or environment variable (e.g. `APP_CACHE_TTL=0s`).

## Kubernetes Deployment (optional)

When deployed inside a cluster, leave `app.kube-config` unset so the default in-cluster service account credentials are used. Set the desired namespace via `app.namespace` or rely on the UI to select it per request. Add RBAC permissions for the Istio CRDs (virtualservices, destinationrules, etc.) plus core `services`.

## Project Layout

- `src/main/java` – Spring Boot application, configuration binding, Kubernetes/Istio loaders, graph builder, and REST controllers.
- `src/main/resources/static` – The single-page canvas UI (HTML/CSS/JS) that renders the routing graph and exposes config inspectors.
- `Dockerfile` – Multi-stage build that compiles the jar and ships a slim Java 21 runtime image.

## API Shape

`GET /api/graph?namespace=<name>` returns:

```json
{
  "namespace": "default",
  "generatedAt": "2025-09-25T10:00:00Z",
  "summary": {
    "nodes": 21,
    "edges": 32,
    "virtualServices": 4,
    "destinationRules": 3,
    "serviceEntries": 2
  },
  "nodes": [
    {
      "id": "host:reviews.default.svc.cluster.local",
      "type": "service",
      "properties": {
        "host": "reviews.default.svc.cluster.local",
        "service": "reviews",
        "resources": [
          {"kind": "Service", "name": "reviews", "namespace": "default", "spec": {"ports": []}}
        ]
      }
    }
  ],
  "edges": [
    {
      "id": "host:productpage.default.svc.cluster.local->host:reviews.default.svc.cluster.local:abc123defg",
      "kind": "traffic",
      "source": "host:productpage.default.svc.cluster.local",
      "target": "host:reviews.default.svc.cluster.local",
      "properties": {
        "protocol": "http",
        "route": {"timeout": "5s"}
      }
    }
  ],
  "warnings": ["VirtualService default/weather http route #1 has no destinations"]
}
```

Errors when contacting the Kubernetes API yield `502 Bad Gateway` with a JSON body `{ "error": "..." }`.

## Development Tips

- Use `kubectl proxy` or `kind` to expose a kube-apiserver endpoint locally and point `--app.kube-config` at your kubeconfig.
- To extend the graph (e.g. include Policies), attach additional resource readers in `IstioResourceLoader` and enrich the node `resources` list in `GraphBuilder`.
- The frontend runs without a build step; edit the files in `src/main/resources/static` and reload your browser while the Spring Boot dev server is running.




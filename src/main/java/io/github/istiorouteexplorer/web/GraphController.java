package io.github.istiorouteexplorer.web;

import io.github.istiorouteexplorer.model.GraphResponse;
import io.github.istiorouteexplorer.service.RouteExplorerException;
import io.github.istiorouteexplorer.service.RouteExplorerService;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private static final Logger log = LoggerFactory.getLogger(GraphController.class);

    private final RouteExplorerService routeExplorerService;

    public GraphController(RouteExplorerService routeExplorerService) {
        this.routeExplorerService = routeExplorerService;
    }

    @GetMapping
    public GraphResponse graph(@RequestParam(name = "namespace", required = false) Optional<String> namespace) {
        return routeExplorerService.buildGraph(namespace.orElse(null));
    }

    @ExceptionHandler(RouteExplorerException.class)
    public ResponseEntity<?> handle(RouteExplorerException ex) {
        log.error("Failed to build graph", ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                java.util.Map.of("error", ex.getMessage())
        );
    }
}

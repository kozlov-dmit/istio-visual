package io.github.istiorouteexplorer.controller;

import io.github.istiorouteexplorer.model.RoutesResponse;
import io.github.istiorouteexplorer.service.RouteExplorerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@CrossOrigin
public class RouteExplorerController {

    private final RouteExplorerService routeExplorerService;

    @GetMapping
    public RoutesResponse getRoutes(@RequestParam(value = "namespace", required = false) String namespace) {
        return routeExplorerService.buildRoutes(namespace);
    }
}

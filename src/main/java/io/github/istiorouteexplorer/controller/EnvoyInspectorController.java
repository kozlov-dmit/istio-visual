package io.github.istiorouteexplorer.controller;

import io.github.istiorouteexplorer.model.envoy.EnvoyConfigResponse;
import io.github.istiorouteexplorer.model.envoy.EnvoyPodsResponse;
import io.github.istiorouteexplorer.service.EnvoyInspectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/envoy")
@RequiredArgsConstructor
@CrossOrigin
public class EnvoyInspectorController {

    private final EnvoyInspectorService envoyInspectorService;

    @GetMapping("/pods")
    public EnvoyPodsResponse listEnvoyPods(@RequestParam(value = "namespace", required = false) String namespace)
            throws IOException {
        return envoyInspectorService.listEnvoyPods(namespace);
    }

    @GetMapping("/pods/{podName}")
    public EnvoyConfigResponse envoyConfig(@PathVariable String podName,
                                            @RequestParam(value = "namespace", required = false) String namespace)
            throws IOException {
        return envoyInspectorService.fetchEnvoyConfig(namespace, podName);
    }
}

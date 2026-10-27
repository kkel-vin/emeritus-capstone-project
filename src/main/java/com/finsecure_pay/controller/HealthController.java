package com.finsecure_pay.controller;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exposes the service health endpoint.
 *
 * @author King Kel Vin
 */

@RestController
public class HealthController {

    private final ObjectProvider<BuildProperties> buildProperties;

    public HealthController(ObjectProvider<BuildProperties> buildProperties) {
        this.buildProperties = buildProperties;
    }

    /**
     * Reports that the service is running, along with its name and build version.
     *
     * @return a JSON map with the keys {@code status}, {@code service} and {@code version}
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "finsecure-pay");
        BuildProperties build = buildProperties.getIfAvailable();
        body.put("version", build != null ? build.getVersion() : "unknown");
        return body;
    }
}

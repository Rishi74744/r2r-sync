package com.r2r.sync.controller;

import com.r2r.sync.entity.ConnectorConfigEntity;
import com.r2r.sync.repository.ConnectorConfigRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
@Tag(name = "Config Management", description = "Get ans Save config")
public class ConfigController {
    private final ConnectorConfigRepository configRepository;

    public ConfigController(ConnectorConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @GetMapping("/{connectorId}")
    public ResponseEntity<ConnectorConfigEntity> getConfig(@PathVariable String connectorId) {
        return configRepository.findByConnectorId(connectorId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Void> saveConfig(@RequestBody ConnectorConfigEntity config) {
        configRepository.save(config);
        return ResponseEntity.ok().build();
    }
}

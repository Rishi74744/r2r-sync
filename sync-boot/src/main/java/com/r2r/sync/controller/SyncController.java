package com.r2r.sync.controller;

import com.r2r.sync.service.impl.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sync")
@Tag(name = "Sync Operations", description = "Triggering and managing synchronization tasks")
public class SyncController {
    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/{recordId}")
    @Operation(summary = "Trigger a sync for a specific record", description = "Dispatches a SyncEvent to the configured connector")
    public ResponseEntity<String> trigger(@PathVariable("recordId") String recordId, 
                                          @RequestParam("connectorId") String connectorId) {
        try {
            syncService.triggerSync(recordId, connectorId);
            return ResponseEntity.ok("Sync triggered for record: " + recordId);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to trigger sync: " + e.getMessage());
        }
    }
}

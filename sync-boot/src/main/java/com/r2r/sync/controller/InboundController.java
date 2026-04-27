package com.r2r.sync.controller;

import com.r2r.sync.dto.RecordDTO;
import com.r2r.sync.service.impl.RecordService;
import com.r2r.sync.events.PollingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inbound")
@Tag(name = "Inbound Integration", description = "Simulating External-to-Internal synchronization via Webhooks and Polling")
public class InboundController {
    private final RecordService recordService;
    private final PollingService pollingService;

    public InboundController(RecordService recordService, PollingService pollingService) {
        this.recordService = recordService;
        this.pollingService = pollingService;
    }

    @PostMapping("/webhook")
    @Operation(summary = "Handle inbound webhook from external system")
    public ResponseEntity<String> handleWebhook(@RequestBody RecordDTO recordDTO) {
        System.out.println("Received inbound webhook for record: " + recordDTO.getId());
        
        recordService.updateRecord(recordDTO.getId(), recordDTO)
                .orElseGet(() -> recordService.createRecord(recordDTO));
                
        return ResponseEntity.ok("Record updated successfully via webhook");
    }

    @PostMapping("/poll/{connectorId}")
    @Operation(summary = "Trigger a manual poll of an external system")
    public ResponseEntity<String> triggerPoll(@PathVariable("connectorId") String connectorId) {
        pollingService.pollExternalSystem(connectorId);
        return ResponseEntity.ok("Polling completed for: " + connectorId);
    }
}

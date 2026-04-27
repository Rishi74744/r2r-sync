package com.r2r.sync.controller;

import com.r2r.sync.entity.AuditEntity;
import com.r2r.sync.entity.SyncEventEntity;
import com.r2r.sync.repository.AuditRepository;
import com.r2r.sync.repository.EventRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/monitoring")
@Tag(name = "Monitoring", description = "Audit and Events APIs")
public class MonitoringController {
    private final EventRepository eventRepository;
    private final AuditRepository auditRepository;

    public MonitoringController(EventRepository eventRepository, AuditRepository auditRepository) {
        this.eventRepository = eventRepository;
        this.auditRepository = auditRepository;
    }

    @GetMapping("/events")
    public ResponseEntity<List<SyncEventEntity>> getEvents() {
        return ResponseEntity.ok(eventRepository.findAll());
    }

    @GetMapping("/audits")
    public ResponseEntity<List<AuditEntity>> getAudits() {
        return ResponseEntity.ok(auditRepository.findAll());
    }
}

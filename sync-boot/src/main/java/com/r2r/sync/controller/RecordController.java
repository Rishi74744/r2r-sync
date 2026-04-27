package com.r2r.sync.controller;

import com.r2r.sync.dto.RecordDTO;
import com.r2r.sync.service.impl.RecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/records")
@Tag(name = "Record Management", description = "CRUD operations for internal records")
public class RecordController {
    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @PostMapping
    @Operation(summary = "Create a new record")
    public ResponseEntity<RecordDTO> create(@RequestBody RecordDTO recordDTO) {
        return ResponseEntity.ok(recordService.createRecord(recordDTO));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get record by ID")
    public ResponseEntity<RecordDTO> get(@PathVariable("id") String id) {
        return recordService.getRecord(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "List all records")
    public ResponseEntity<List<RecordDTO>> list() {
        return ResponseEntity.ok(recordService.getAllRecords());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing record")
    public ResponseEntity<RecordDTO> update(@PathVariable("id") String id, @RequestBody RecordDTO recordDTO) {
        return recordService.updateRecord(id, recordDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a record")
    public ResponseEntity<Void> delete(@PathVariable("id") String id) {
        if (recordService.deleteRecord(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}

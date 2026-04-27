package com.r2r.sync.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEntity {
    private String id;
    private String eventId;
    private String action;
    private String status;
    private String details;
    private long timestamp;
}

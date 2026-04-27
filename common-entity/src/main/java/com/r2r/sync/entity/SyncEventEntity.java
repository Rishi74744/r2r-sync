package com.r2r.sync.entity;

import com.r2r.sync.enums.EventStatus;
import com.r2r.sync.enums.Priority;
import com.r2r.sync.enums.SourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncEventEntity {
    private String eventId;
    private String recordId;
    private String connectorId;
    private Map<String, Object> payload;
    private EventStatus status;
    private int retryCount;
    private int reconcileCount;
    private long createdAt;
    private long updatedAt;
    private int version;
    private Priority priority;
    private SourceType source;
}

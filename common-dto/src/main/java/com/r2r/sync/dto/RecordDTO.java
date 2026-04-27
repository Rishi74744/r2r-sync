package com.r2r.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Record represents the domain model for a synchronized object.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordDTO {
    private String id;
    private String type;
    private Map<String, Object> data;
    private int version;
    private long updatedAt;
}

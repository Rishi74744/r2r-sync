package com.r2r.sync.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordEntity {
    private String id;
    private String type;
    private Map<String, Object> data;
    private int version;
    private long updatedAt;
}

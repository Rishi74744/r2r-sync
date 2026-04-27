package com.r2r.sync.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ApiResponse represents the standard response from an external connector.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiResponseDTO {
    private String status;
    private boolean retryable;
    private Object data;
    private String error;
    private String errorCode;
}

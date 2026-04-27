package com.r2r.sync.lib.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2r.sync.dto.ApiResponseDTO;

import java.io.InputStream;

public class JsonUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static ApiResponseDTO readMockResponse(String connectorId, String scenario) {
        try {
            String path = "mocks/" + connectorId + "/" + scenario + ".json";
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
            if (is == null) {
                System.out.println("Mock file not found: " + path);
                return ApiResponseDTO.builder().status("SUCCESS").build();
            }
            return mapper.readValue(is, ApiResponseDTO.class);
        } catch (Exception e) {
            System.err.println("Error reading mock response: " + e.getMessage());
            return ApiResponseDTO.builder().status("FAILURE").build();
        }
    }
}

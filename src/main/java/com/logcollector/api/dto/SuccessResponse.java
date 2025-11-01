package com.logcollector.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard success response DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuccessResponse {
    private boolean success;
    private String message;

    public static SuccessResponse of(String message) {
        return new SuccessResponse(true, message);
    }
}

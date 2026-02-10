package com.fintech.ewallet.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Standard API error response format.
 * All API errors return this structure for consistency.
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String code;
    private final String message;
    private final List<FieldError> details;
    private final String path;

    /**
     * Represents a single field validation error.
     */
    @Getter
    @AllArgsConstructor
    public static class FieldError {
        private final String field;
        private final String message;
    }
}

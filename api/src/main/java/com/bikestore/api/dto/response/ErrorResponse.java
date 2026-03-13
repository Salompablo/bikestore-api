package com.bikestore.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Standardized error response object")
public record ErrorResponse(

        @Schema(description = "HTTP Status Code", example = "400")
        int status,

        @Schema(description = "HTTP Status Phrase", example = "Bad Request")
        String error,

        @Schema(description = "Detailed error message", example = "Invalid input data provided.")
        String message,

        @Schema(description = "Timestamp when the error occurred", example = "2026-03-10T15:30:00.000Z")
        LocalDateTime timestamp
) {
}

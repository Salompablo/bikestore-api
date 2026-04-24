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
        LocalDateTime timestamp,

        @Schema(description = """
                Machine-readable error code for client-side message mapping. Possible values:
                - RESERVED_TEMPORARILY: one or more products lack available stock because units are \
                temporarily reserved by other active orders. Retry after retryAfterSeconds.""",
                example = "RESERVED_TEMPORARILY",
                nullable = true)
        String errorCode,

        @Schema(description = "Suggested seconds to wait before retrying. Present when errorCode is RESERVED_TEMPORARILY.",
                example = "600",
                nullable = true)
        Integer retryAfterSeconds
) {
}

package com.bikestore.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Generic message response")
public record MessageResponse(

        @Schema(description = "Response message", example = "Operation completed successfully.")
        String message
) {
}

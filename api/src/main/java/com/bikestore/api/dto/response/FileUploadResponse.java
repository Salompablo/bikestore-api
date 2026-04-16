package com.bikestore.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "File upload response")
public record FileUploadResponse(

        @Schema(description = "URL of the uploaded file", example = "https://xbucket.s3.amazonaws.com/123e4567-e89b-12d3-a456-426614174000.jpg")
        String url
) {
}
package com.bikestore.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pagination details")
public record PageMetaData(
        @Schema(description = "Number of items requested per page", example = "10")
        int size,

        @Schema(description = "Current page number (0-indexed)", example = "0")
        int number,

        @Schema(description = "Total number of elements across all pages", example = "50")
        long totalElements,

        @Schema(description = "Total number of pages available", example = "5")
        int totalPages
) {
}
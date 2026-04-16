package com.bikestore.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Response payload representing a product review")
public record ReviewResponse(
        @Schema(description = "Unique review ID", example = "1")
        Long id,

        @Schema(description = "Rating from 1 to 5", example = "4")
        Integer rating,

        @Schema(description = "Review comment", example = "Great bike, very comfortable!")
        String comment,

        @Schema(description = "Date and time when the review was created")
        LocalDateTime createdAt,

        @Schema(description = "First name of the reviewer", example = "John")
        String userFirstName,

        @Schema(description = "Last name of the reviewer", example = "Doe")
        String userLastName
) {
}

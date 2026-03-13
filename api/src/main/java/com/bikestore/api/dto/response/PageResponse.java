package com.bikestore.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Generic paginated response")
public record PageResponse<T>(

        @Schema(description = "List of items in the current page")
        List<T> content,

        @Schema(description = "Pagination metadata")
        PageMetaData page
) {

    public static <T> PageResponse<T> of(Page<T> springPage) {
        return new PageResponse<>(
                springPage.getContent(),
                new PageMetaData(
                        springPage.getSize(),
                        springPage.getNumber(),
                        springPage.getTotalElements(),
                        springPage.getTotalPages()
                )
        );
    }
}
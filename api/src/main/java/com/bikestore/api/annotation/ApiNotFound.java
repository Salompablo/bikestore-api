package com.bikestore.api.annotation;

import com.bikestore.api.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(responseCode = "404", description = "Resource not found",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                        {
                            "status": 404,
                            "error": "Not Found",
                            "message": "The requested resource could not be found.",
                            "timestamp": "2026-03-13T16:00:00.000Z"
                        }
                        """)))
public @interface ApiNotFound {
}

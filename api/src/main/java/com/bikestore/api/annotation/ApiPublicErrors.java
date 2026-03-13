package com.bikestore.api.annotation;

import com.bikestore.api.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class),
                        examples = @ExampleObject(value = """
                                {
                                    "status": 400,
                                    "error": "Bad Request",
                                    "message": "Invalid input data provided.",
                                    "timestamp": "2026-03-13T16:00:00.000Z"
                                }
                                """))),
        @ApiResponse(responseCode = "500", description = "Internal Server Error",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class),
                        examples = @ExampleObject(value = """
                                {
                                    "status": 500,
                                    "error": "Internal Server Error",
                                    "message": "An unexpected internal error occurred. Please try again later.",
                                    "timestamp": "2026-03-13T16:00:00.000Z"
                                }
                                """)))
})
public @interface ApiPublicErrors {
}
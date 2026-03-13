package com.bikestore.api.controller;

import com.bikestore.api.annotation.ApiAdminErrors;
import com.bikestore.api.dto.response.ErrorResponse;
import com.bikestore.api.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "Endpoints for managing media and file uploads to AWS S3")
public class FileController {

    private final S3Service s3Service;

    @Operation(summary = "Upload a file", description = "Uploads a single file to AWS S3. Maximum file size is 5MB. Requires ADMIN privileges.")
    @ApiResponse(
            responseCode = "200",
            description = "File successfully uploaded",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                {
                    "url": "https://xbucket.s3.amazonaws.com/123e4567-e89b-12d3-a456-426614174000.jpg"
                }
                """))
    )
    @ApiResponse(
            responseCode = "413",
            description = "Payload Too Large (File exceeds 5MB limit)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = """
                {
                    "status": 413,
                    "error": "Payload Too Large",
                    "message": "File size limit exceeded. The uploaded file must be less than 5MB.",
                    "timestamp": "2026-03-13T16:00:00.000Z"
                }
                """))
    )
    @ApiAdminErrors
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadFile(
            @Parameter(description = "Image file to upload", required = true)
            @RequestParam("file") MultipartFile file) {

        String fileUrl = s3Service.uploadFile(file);
        return ResponseEntity.ok(Map.of("url", fileUrl));
    }
}

package com.bikestore.api.controller;

import com.bikestore.api.dto.request.ContactRequest;
import com.bikestore.api.dto.response.ErrorResponse;
import com.bikestore.api.dto.response.MessageResponse;
import com.bikestore.api.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contact")
@RequiredArgsConstructor
@Tag(name = "Contact", description = "Endpoint for submitting contact form messages protected by Google reCAPTCHA v3")
public class ContactController {

    private final ContactService contactService;

    @Operation(
            summary = "Submit a contact message",
            description = "Validates the Google reCAPTCHA v3 token, persists the contact message, "
                    + "and triggers an asynchronous admin email notification."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Contact message received and processed successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request – Form validation errors "
                            + "(e.g. invalid email, message too long, required fields missing)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict – Google reCAPTCHA validation failed "
                            + "(invalid token, score below threshold, or bot-like behaviour detected)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<MessageResponse> submitContactMessage(@Valid @RequestBody ContactRequest request) {
        contactService.processContactRequest(request);
        return ResponseEntity.ok(new MessageResponse("Your message has been received. We will get back to you soon."));
    }
}

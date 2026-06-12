package com.bikestore.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Maps the JSON response returned by the Google reCAPTCHA v3 verification API.
 */
public record RecaptchaResponse(
        boolean success,
        Double score,
        String action,
        @JsonProperty("challenge_ts") String challengeTs,
        String hostname,
        @JsonProperty("error-codes") List<String> errorCodes
) {
}

package com.bikestore.api.service;

import com.bikestore.api.dto.response.RecaptchaResponse;
import com.bikestore.api.exception.ConflictException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Validates Google reCAPTCHA v3 tokens by calling the Google verification API.
 */
@Service
@Slf4j
public class CaptchaValidationService {

    private static final String EXPECTED_ACTION = "submit_contact";

    private final RestClient restClient;
    private final String secretKey;
    private final String verifyUrl;
    private final double minimumScore;

    public CaptchaValidationService(
            @Value("${recaptcha.secret-key}") String secretKey,
            @Value("${recaptcha.verify-url}") String verifyUrl,
            @Value("${recaptcha.minimum-score}") double minimumScore
    ) {
        this.restClient = RestClient.create();
        this.secretKey = secretKey;
        this.verifyUrl = verifyUrl;
        this.minimumScore = minimumScore;
    }

    /**
     * Validates the provided reCAPTCHA token against the Google API.
     * Throws {@link ConflictException} if the token is invalid, the score is below the
     * configured threshold, or the action does not match the expected value.
     *
     * @param token the reCAPTCHA token submitted by the client
     */
    public void validateToken(String token) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("secret", secretKey);
        formData.add("response", token);

        RecaptchaResponse response = restClient.post()
                .uri(verifyUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(RecaptchaResponse.class);

        if (response == null || !response.success()) {
            log.warn("reCAPTCHA verification failed. Error codes: {}",
                    response != null ? response.errorCodes() : "null response");
            throw new ConflictException("reCAPTCHA verification failed. Please try again.");
        }

        if (response.score() == null || response.score() < minimumScore) {
            throw new ConflictException("Automated bot behavior detected. Request blocked.");
        }

        if (!EXPECTED_ACTION.equals(response.action())) {
            log.warn("reCAPTCHA action mismatch. Expected '{}', got '{}'",
                    EXPECTED_ACTION, response.action());
            throw new ConflictException("reCAPTCHA action mismatch. Token is not valid for this form.");
        }

        log.debug("reCAPTCHA validation passed. Score: {}, action: {}", response.score(), response.action());
    }
}

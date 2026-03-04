package com.bikestore.api.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Value("${resend.api.key}")
    private String resendApiKey;

    public void sendVerificationEmail(String toEmail, String verificationCode) {
        Resend resend = new Resend(resendApiKey);

        String htmlContent = String.format(
                "<h2>¡Bienvenido a BikeStore!</h2>" +
                        "<p>Tu código de verificación de 6 dígitos es:</p>" +
                        "<h3>%s</h3>" +
                        "<p>Este código expirará en 15 minutos.</p>",
                verificationCode
        );

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("BikeStore <onboarding@resend.dev>")
                .to(toEmail)
                .subject("Verifica tu cuenta en BikeStore")
                .html(htmlContent)
                .build();

        try {
            resend.emails().send(params);
            log.info("Verification email sent successfully to: {}", toEmail);
        } catch (ResendException e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException("Error sending verification email");
        }
    }
}

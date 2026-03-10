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
                "<h2>¡Bienvenido a Bikes Asaro!</h2>" +
                        "<p>Tu código de verificación de 6 dígitos es:</p>" +
                        "<h3>%s</h3>" +
                        "<p>Este código expirará en 15 minutos.</p>",
                verificationCode
        );

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("Bikes Asaro <onboarding@resend.dev>")
                .to(toEmail)
                .subject("Verifica tu cuenta en Bikes Asaro")
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

    public void sendReactivationEmail(String toEmail, String verificationCode) {
        Resend resend = new Resend(resendApiKey);

        String htmlContent = String.format(
                "<h2>¡Te extrañábamos en Bikes Asaro!</h2>" +
                        "<p>Recibimos una solicitud para reactivar tu cuenta. Tu código de seguridad es:</p>" +
                        "<h3>%s</h3>" +
                        "<p>Si no fuiste tú, simplemente ignora este correo.</p>",
                verificationCode
        );

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("Bikes Asaro <onboarding@resend.dev>")
                .to(toEmail)
                .subject("Reactiva tu cuenta en Bikes Asaro")
                .html(htmlContent)
                .build();

        try {
            resend.emails().send(params);
        } catch (com.resend.core.exception.ResendException e) {
            throw new RuntimeException("Error sending reactivation email");
        }
    }

    public void sendPasswordResetEmail(String toEmail, String resetCode) {
        Resend resend = new Resend(resendApiKey);

        String htmlContent = String.format(
                "<h2>Recuperación de contraseña - Bikes Asaro</h2>" +
                        "<p>Hemos recibido una solicitud para restablecer tu contraseña.</p>" +
                        "<p>Tu código de seguridad es:</p>" +
                        "<h3>%s</h3>" +
                        "<p>Este código expirará en 15 minutos.</p>" +
                        "<p>Si no fuiste tú, ignora este correo.</p>",
                resetCode
        );

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("Bikes Asaro <onboarding@resend.dev>")
                .to(toEmail)
                .subject("Recuperación de contraseña en Bikes Asaro")
                .html(htmlContent)
                .build();

        try {
            resend.emails().send(params);
            log.info("Password reset email sent successfully to: {}", toEmail);
        } catch (ResendException e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
        }
    }
}

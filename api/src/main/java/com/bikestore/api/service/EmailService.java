package com.bikestore.api.service;

import com.bikestore.api.entity.enums.DeliveryMethod;
import com.bikestore.api.event.OrderPaidNotificationData;
import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.stream.Collectors;

@Service
@Slf4j
public class EmailService {

    @Value("${resend.api.key}")
    private String resendApiKey;

    public void sendVerificationEmail(String toEmail, String verificationCode) {
        Resend resend = new Resend(resendApiKey);

        log.info("📧 [DEV MODE] Generating email to: {}", toEmail);
        log.info("🔑 Verification code: {}", verificationCode);

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
            log.info("✅ Verification email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Failed to send verification email (likely due to testing mode): {}", e.getMessage());
            log.warn("⚠️ Continuing local process. Use the verification code printed above.");
        }
    }

    public void sendReactivationEmail(String toEmail, String verificationCode) {
        Resend resend = new Resend(resendApiKey);

        log.info("📧 [DEV MODE] Generating reactivation email to: {}", toEmail);
        log.info("🔑 Reactivation code: {}", verificationCode);

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
            log.info("✅ Reactivation email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Failed to send reactivation email (likely due to testing mode): {}", e.getMessage());
            log.warn("⚠️ Continuing local process. Use the reactivation code printed above.");
        }
    }

    public void sendPasswordResetEmail(String toEmail, String resetCode) {
        Resend resend = new Resend(resendApiKey);

        log.info("📧 [DEV MODE] Generating password reset email to: {}", toEmail);
        log.info("🔑 Password reset code: {}", resetCode);

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
            log.info("✅ Password reset email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Failed to send password reset email (likely due to testing mode): {}", e.getMessage());
            log.warn("⚠️ Continuing local process. Use the password reset code printed above.");
        }
    }

    public void sendAdminOrderNotification(String toEmail, OrderPaidNotificationData orderData) {
        Resend resend = new Resend(resendApiKey);

        String productsHtml = orderData.items().stream()
                .map(item -> "<li>" + HtmlUtils.htmlEscape(item.productName()) + " x" + item.quantity() + "</li>")
                .collect(Collectors.joining());

        String deliveryMethod = orderData.deliveryMethod() == DeliveryMethod.SHIPPING ? "Envío" : "Retiro";
        String contactPhone = orderData.contactPhone() == null || orderData.contactPhone().isBlank()
                ? "No informado"
                : orderData.contactPhone();

        String htmlContent = String.format(
                "<h2>Nueva orden pagada</h2>" +
                        "<p><strong>Orden ID:</strong> %d</p>" +
                        "<p><strong>Cliente:</strong> %s</p>" +
                        "<p><strong>Email:</strong> %s</p>" +
                        "<p><strong>Teléfono:</strong> %s</p>" +
                        "<p><strong>Productos:</strong></p>" +
                        "<ul>%s</ul>" +
                        "<p><strong>Total:</strong> $%s</p>" +
                        "<p><strong>Método de entrega:</strong> %s</p>",
                orderData.orderId(),
                HtmlUtils.htmlEscape(orderData.customerFullName()),
                HtmlUtils.htmlEscape(orderData.customerEmail()),
                HtmlUtils.htmlEscape(contactPhone),
                productsHtml,
                orderData.totalAmount(),
                deliveryMethod
        );

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("Bikes Asaro <onboarding@resend.dev>")
                .to(toEmail)
                .subject("Nueva orden pagada #" + orderData.orderId())
                .html(htmlContent)
                .build();

        try {
            resend.emails().send(params);
            log.info("✅ Admin order notification email sent successfully for order {}", orderData.orderId());
        } catch (Exception e) {
            log.error("❌ Failed to send admin order notification email for order {}", orderData.orderId(), e);
            throw new RuntimeException("Failed to send admin order notification email", e);
        }
    }
}

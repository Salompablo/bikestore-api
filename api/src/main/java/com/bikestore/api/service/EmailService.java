package com.bikestore.api.service;

import com.bikestore.api.dto.data.CustomerOrderConfirmationData;
import com.bikestore.api.entity.enums.DeliveryMethod;
import com.bikestore.api.event.OrderPaidNotificationData;
import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class EmailService {

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.store.address}")
    private String storeAddress;

    @Value("${app.email.placeholder-image-url:}")
    private String placeholderImageUrl;

    // -------------------------------------------------------------------------
    // Public send methods
    // -------------------------------------------------------------------------

    public void sendVerificationEmail(String toEmail, String verificationCode) {
        log.info("📧 [DEV MODE] Generating email to: {}", toEmail);
        log.info("🔑 Verification code: {}", verificationCode);

        String content =
                "<p style=\"margin:0 0 16px 0;font-size:16px;color:#374151;\">¡Bienvenido a <strong>Bikes Asaro</strong>! " +
                "Para activar tu cuenta, utilizá el siguiente código de verificación de 6 dígitos:</p>" +
                buildCodeBox(verificationCode) +
                "<p style=\"margin:16px 0 0 0;font-size:14px;color:#6b7280;\">Este código expirará en <strong>15 minutos</strong>. " +
                "Si no creaste una cuenta, podés ignorar este correo.</p>";

        sendHtmlEmail(toEmail, "Verifica tu cuenta en Bikes Asaro",
                buildEmailTemplate("Verificación de cuenta", content));
        log.info("✅ Verification email sent successfully to: {}", toEmail);
    }

    public void sendReactivationEmail(String toEmail, String verificationCode) {
        log.info("📧 [DEV MODE] Generating reactivation email to: {}", toEmail);
        log.info("🔑 Reactivation code: {}", verificationCode);

        String content =
                "<p style=\"margin:0 0 16px 0;font-size:16px;color:#374151;\">¡Te extrañábamos! " +
                "Recibimos una solicitud para <strong>reactivar tu cuenta</strong>. Tu código de seguridad es:</p>" +
                buildCodeBox(verificationCode) +
                "<p style=\"margin:16px 0 0 0;font-size:14px;color:#6b7280;\">Si no fuiste vos, simplemente ignorá este correo.</p>";

        sendHtmlEmail(toEmail, "Reactiva tu cuenta en Bikes Asaro",
                buildEmailTemplate("Reactivación de cuenta", content));
        log.info("✅ Reactivation email sent successfully to: {}", toEmail);
    }

    public void sendPasswordResetEmail(String toEmail, String resetCode) {
        log.info("📧 [DEV MODE] Generating password reset email to: {}", toEmail);
        log.info("🔑 Password reset code: {}", resetCode);

        String content =
                "<p style=\"margin:0 0 8px 0;font-size:16px;color:#374151;\">Recibimos una solicitud para " +
                "<strong>restablecer la contraseña</strong> de tu cuenta.</p>" +
                "<p style=\"margin:0 0 16px 0;font-size:16px;color:#374151;\">Tu código de seguridad es:</p>" +
                buildCodeBox(resetCode) +
                "<p style=\"margin:16px 0 0 0;font-size:14px;color:#6b7280;\">Este código expirará en <strong>15 minutos</strong>. " +
                "Si no solicitaste el cambio, ignorá este correo.</p>";

        sendHtmlEmail(toEmail, "Recuperación de contraseña en Bikes Asaro",
                buildEmailTemplate("Recuperación de contraseña", content));
        log.info("✅ Password reset email sent successfully to: {}", toEmail);
    }

    public void sendAdminOrderNotification(String toEmail, OrderPaidNotificationData orderData) {
        String deliveryMethod = orderData.deliveryMethod() == DeliveryMethod.SHIPPING ? "Envío a domicilio" : "Retiro en tienda";
        String contactPhone = orderData.contactPhone() == null || orderData.contactPhone().isBlank()
                ? "No informado"
                : orderData.contactPhone();

        String productRows = orderData.items().stream()
                .map(item -> String.format(
                        "<tr>" +
                        "<td style=\"padding:10px 8px;border-bottom:1px solid #e5e7eb;color:#374151;\">%s</td>" +
                        "<td style=\"padding:10px 8px;border-bottom:1px solid #e5e7eb;color:#374151;text-align:center;\">%d</td>" +
                        "</tr>",
                        HtmlUtils.htmlEscape(item.productName()),
                        item.quantity()))
                .collect(Collectors.joining());

        String content = String.format(
                "<table style=\"width:100%%;border-collapse:collapse;margin-bottom:24px;font-size:15px;\">" +
                "<tr><td style=\"padding:8px 0;color:#6b7280;width:40%%;\">Orden ID</td>" +
                    "<td style=\"padding:8px 0;color:#374151;font-weight:bold;\">#%d</td></tr>" +
                "<tr><td style=\"padding:8px 0;color:#6b7280;\">Cliente</td>" +
                    "<td style=\"padding:8px 0;color:#374151;\">%s</td></tr>" +
                "<tr><td style=\"padding:8px 0;color:#6b7280;\">Email</td>" +
                    "<td style=\"padding:8px 0;color:#374151;\">%s</td></tr>" +
                "<tr><td style=\"padding:8px 0;color:#6b7280;\">Teléfono</td>" +
                    "<td style=\"padding:8px 0;color:#374151;\">%s</td></tr>" +
                "</table>" +

                "<p style=\"margin:0 0 8px 0;font-size:15px;color:#374151;font-weight:bold;\">Productos</p>" +
                "<table style=\"width:100%%;border-collapse:collapse;margin-bottom:24px;font-size:15px;\">" +
                "<thead>" +
                "<tr style=\"background-color:#f3f4f6;\">" +
                "<th style=\"padding:10px 8px;text-align:left;color:#374151;border-bottom:2px solid #e5e7eb;\">Producto</th>" +
                "<th style=\"padding:10px 8px;text-align:center;color:#374151;border-bottom:2px solid #e5e7eb;\">Cantidad</th>" +
                "</tr>" +
                "</thead>" +
                "<tbody>%s</tbody>" +
                "</table>" +

                "<table style=\"width:100%%;border-collapse:collapse;font-size:15px;\">" +
                "<tr style=\"background-color:#fef9c3;\">" +
                "<td style=\"padding:12px 8px;font-weight:bold;color:#111827;\">Total</td>" +
                "<td style=\"padding:12px 8px;font-weight:bold;color:#111827;text-align:right;\">$%s</td>" +
                "</tr>" +
                "<tr style=\"background-color:#fef9c3;\">" +
                "<td style=\"padding:12px 8px;font-weight:bold;color:#111827;\">Método de entrega</td>" +
                "<td style=\"padding:12px 8px;font-weight:bold;color:#111827;text-align:right;\">%s</td>" +
                "</tr>" +
                "</table>",
                orderData.orderId(),
                HtmlUtils.htmlEscape(orderData.customerFullName()),
                HtmlUtils.htmlEscape(orderData.customerEmail()),
                HtmlUtils.htmlEscape(contactPhone),
                productRows,
                orderData.totalAmount(),
                deliveryMethod
        );

        String html = buildEmailTemplate("Nueva orden pagada #" + orderData.orderId(), content);

        Resend resend = new Resend(resendApiKey);
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("Bikes Asaro <onboarding@resend.dev>")
                .to(toEmail)
                .subject("Nueva orden pagada #" + orderData.orderId())
                .html(html)
                .build();

        try {
            resend.emails().send(params);
            log.info("✅ Admin order notification email sent successfully for order {}", orderData.orderId());
        } catch (Exception e) {
            log.error("❌ Failed to send admin order notification email for order {}", orderData.orderId(), e);
            throw new RuntimeException("Failed to send admin order notification email", e);
        }
    }

    public void sendCustomerOrderConfirmation(CustomerOrderConfirmationData data) {
        String customerName = safeText(data.customerName(), "Cliente");
        String orderDetailUrl = buildFrontendUrl("/orders/" + data.orderId());
        String contactUrl = buildFrontendUrl("/contacto");
        String deliverySummary = buildDeliverySummary(data);
        String imageGroup = buildProductPreviewImages(data.productPreviewImages());

        String content = String.format(
                "<div style=\"text-align:center;padding-top:8px;\">" +
                "<div style=\"width:64px;height:64px;border-radius:50%%;background-color:#22c55e;margin:0 auto 16px auto;" +
                "line-height:64px;text-align:center;font-size:34px;font-weight:bold;color:#ffffff;\">&#10003;</div>" +
                "<h1 style=\"margin:0 0 10px 0;font-size:28px;line-height:1.2;color:#111827;font-weight:700;\">" +
                "&#161;Gracias por tu compra, %s!</h1>" +
                "<p style=\"margin:0 0 24px 0;font-size:16px;line-height:1.5;color:#4b5563;\">" +
                "Tu pago fue confirmado con &eacute;xito y ya estamos preparando tu pedido.</p>" +
                "%s" +
                "<div style=\"margin:0 0 24px 0;padding:20px;border:1px solid #e5e7eb;border-radius:12px;background-color:#f9fafb;text-align:left;\">" +
                "<p style=\"margin:0 0 10px 0;font-size:14px;color:#6b7280;\">Orden</p>" +
                "<p style=\"margin:0 0 16px 0;font-size:24px;color:#111827;font-weight:700;\">#%d</p>" +
                "<p style=\"margin:0 0 8px 0;font-size:14px;color:#6b7280;\">Total abonado</p>" +
                "<p style=\"margin:0 0 18px 0;font-size:30px;color:#111827;font-weight:700;\">%s</p>" +
                "<p style=\"margin:0 0 6px 0;font-size:14px;color:#6b7280;\">Entrega</p>" +
                "<p style=\"margin:0;font-size:16px;line-height:1.5;color:#374151;font-weight:600;\">%s</p>" +
                "</div>" +
                "<div style=\"text-align:center;margin:0 0 28px 0;\">" +
                "<a href=\"%s\" style=\"display:inline-block;background-color:#1f2937;color:#ffffff;font-size:16px;" +
                "font-weight:700;text-decoration:none;padding:14px 28px;border-radius:999px;\">Ver detalle y estado</a>" +
                "</div>" +
                "<p style=\"margin:0 0 10px 0;font-size:14px;line-height:1.6;color:#374151;text-align:center;\">" +
                "Si ten&eacute;s un problema, estamos para ayudarte. " +
                "<a href=\"%s\" style=\"color:#1f2937;font-weight:700;text-decoration:none;\">Contactanos</a></p>" +
                "<p style=\"margin:0;font-size:12px;line-height:1.6;color:#9ca3af;text-align:center;\">" +
                "Te enviamos este correo porque el e-mail %s est&aacute; asociado a una cuenta de Bikes Asaro.</p>" +
                "</div>",
                HtmlUtils.htmlEscape(customerName),
                imageGroup,
                data.orderId(),
                formatCurrency(data.totalAmount()),
                deliverySummary,
                orderDetailUrl,
                contactUrl,
                HtmlUtils.htmlEscape(safeText(data.customerEmail(), "sin-email"))
        );

        sendHtmlEmail(
                data.customerEmail(),
                "Compra confirmada en Bikes Asaro #" + data.orderId(),
                buildEmailTemplate("Compra confirmada", content)
        );
        log.info("✅ Customer order confirmation email processed for order {}", data.orderId());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void sendHtmlEmail(String toEmail, String subject, String html) {
        Resend resend = new Resend(resendApiKey);
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("Bikes Asaro <onboarding@resend.dev>")
                .to(toEmail)
                .subject(subject)
                .html(html)
                .build();
        try {
            resend.emails().send(params);
        } catch (Exception e) {
            log.error("❌ Failed to send email to {} (subject: {}): {}", toEmail, subject, e.getMessage());
            log.warn("⚠️ Continuing local process.");
        }
    }

    private String buildEmailTemplate(String title, String content) {
        return "<!DOCTYPE html>" +
               "<html lang=\"es\">" +
               "<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">" +
               "<title>" + HtmlUtils.htmlEscape(title) + "</title></head>" +
               "<body style=\"margin:0;padding:0;background-color:#f9fafb;font-family:Arial,Helvetica,sans-serif;\">" +
               "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#f9fafb;padding:32px 16px;\">" +
               "<tr><td align=\"center\">" +
               "<table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;width:100%;background-color:#ffffff;" +
               "border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,0.08);overflow:hidden;\">" +

               "<tr><td style=\"background-color:#1f2937;padding:24px 32px;border-bottom:4px solid #facc15;\">" +
               "<span style=\"font-size:22px;font-weight:bold;color:#facc15;letter-spacing:1px;\">Bikes Asaro</span>" +
               "</td></tr>" +

               "<tr><td style=\"padding:24px 32px 0 32px;\">" +
               "<h2 style=\"margin:0 0 16px 0;font-size:20px;color:#1f2937;border-bottom:2px solid #facc15;padding-bottom:10px;\">" +
               HtmlUtils.htmlEscape(title) + "</h2>" +
               "</td></tr>" +

               "<tr><td style=\"padding:0 32px 32px 32px;\">" + content + "</td></tr>" +

               "<tr><td style=\"background-color:#f3f4f6;padding:16px 32px;border-top:1px solid #e5e7eb;text-align:center;\">" +
               "<p style=\"margin:0 0 4px 0;font-size:12px;color:#6b7280;\">Bikes Asaro &mdash; " +
               HtmlUtils.htmlEscape(safeText(storeAddress, "Santa Fe 2611, Mar del Plata, Provincia de Buenos Aires")) +
               "</p>" +
               "<p style=\"margin:0;font-size:12px;color:#9ca3af;\">Este es un correo automático, por favor no respondas a este mensaje.</p>" +
               "</td></tr>" +

               "</table>" +
               "</td></tr></table>" +
               "</body></html>";
    }

    private String buildCodeBox(String code) {
        return "<div style=\"text-align:center;margin:24px 0;\">" +
               "<span style=\"display:inline-block;background-color:#facc15;color:#111827;font-size:28px;" +
               "font-weight:bold;letter-spacing:8px;padding:14px 32px;border-radius:8px;\">" +
               HtmlUtils.htmlEscape(code) +
               "</span>" +
               "</div>";
    }

    private String buildProductPreviewImages(List<String> productPreviewImages) {
        List<String> sanitizedImages = productPreviewImages == null
                ? List.of()
                : productPreviewImages.stream()
                .map(this::sanitizeHttpUrl)
                .filter(Objects::nonNull)
                .limit(3)
                .toList();

        if (sanitizedImages.isEmpty()) {
            String placeholder = sanitizeHttpUrl(placeholderImageUrl);
            if (placeholder == null) {
                return "";
            }
            sanitizedImages = List.of(placeholder);
        }

        List<String> previewImages = sanitizedImages;
        return "<div style=\"text-align:center;margin:0 0 24px 0;\"><div style=\"display:inline-block;\">" +
                IntStream.range(0, previewImages.size())
                        .mapToObj(index -> String.format(
                                "<img src=\"%s\" alt=\"Producto %d\" width=\"60\" height=\"60\" " +
                                "style=\"width:60px;height:60px;border-radius:50%%;border:3px solid #ffffff;" +
                                "object-fit:cover;display:inline-block;vertical-align:middle;%s\" />",
                                previewImages.get(index),
                                index + 1,
                                index == 0 ? "" : "margin-left:-20px;"
                        ))
                        .collect(Collectors.joining()) +
                "</div></div>";
    }

    private String buildDeliverySummary(CustomerOrderConfirmationData data) {
        if (data.deliveryMethod() == DeliveryMethod.SHIPPING) {
            String address = safeText(data.shippingAddress(), "Dirección no disponible");
            String zipCode = safeText(data.zipCode(), "");
            String fullAddress = zipCode.isBlank() ? address : address + " (" + zipCode + ")";
            return "Envío a domicilio<br><span style=\"font-weight:400;color:#4b5563;\">" +
                    HtmlUtils.htmlEscape(fullAddress) + "</span>";
        }

        return "Retiro en local<br><span style=\"font-weight:400;color:#4b5563;\">" +
                HtmlUtils.htmlEscape(safeText(storeAddress, "Dirección de tienda no disponible")) + "</span>";
    }

    private String buildFrontendUrl(String path) {
        String normalizedBase = frontendUrl == null ? "" : frontendUrl.trim();
        if (normalizedBase.endsWith("/")) {
            normalizedBase = normalizedBase.substring(0, normalizedBase.length() - 1);
        }

        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        String absoluteUrl = normalizedBase + normalizedPath;
        String sanitizedUrl = sanitizeHttpUrl(absoluteUrl);
        return sanitizedUrl != null ? sanitizedUrl : "#";
    }

    private String sanitizeHttpUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return null;
            }
            return HtmlUtils.htmlEscape(uri.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String formatCurrency(BigDecimal amount) {
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"));
        currencyFormatter.setCurrency(Currency.getInstance("ARS"));
        return HtmlUtils.htmlEscape(currencyFormatter.format(amount == null ? BigDecimal.ZERO : amount));
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}

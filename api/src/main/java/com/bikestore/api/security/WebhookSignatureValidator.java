package com.bikestore.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Component
public class WebhookSignatureValidator {

    @Value("${mercadopago.webhook-secret}")
    private String webhookSecret;

    public boolean isValid(String xSignature, String xRequestId, String dataId) {

        if (xSignature == null || xRequestId == null || dataId == null) {
            return false;
        }

        try {
            String[] parts = xSignature.split(",");
            String ts = null;
            String v1 = null;

            for (String part : parts) {
                if (part.startsWith("ts=")) ts = part.substring(3);
                if (part.startsWith("v1=")) v1 = part.substring(3);
            }

            if (ts == null || v1 == null) return false;

            String manifest = String.format("id:%s;request-id:%s;ts:%s;", dataId, xRequestId, ts);

            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hashBytes = sha256Hmac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString().equals(v1);

        } catch (Exception e) {
            return false;
        }
    }
}

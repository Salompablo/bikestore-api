package com.bikestore.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
            List<String> v1Signatures = new ArrayList<>();

            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.startsWith("ts=")) ts = trimmed.substring(3);
                if (trimmed.startsWith("v1=")) v1Signatures.add(trimmed.substring(3));
            }

            if (ts == null || v1Signatures.isEmpty()) return false;

            String manifest = String.format("id:%s;request-id:%s;ts:%s;", dataId, xRequestId, ts);

            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.trim().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hashBytes = sha256Hmac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            String computed = hexString.toString();
            return v1Signatures.contains(computed);

        } catch (Exception e) {
            return false;
        }
    }
}

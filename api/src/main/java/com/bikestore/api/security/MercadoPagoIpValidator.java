package com.bikestore.api.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Validates that a webhook/IPN request originates from a known Mercado Pago IP address or CIDR range.
 *
 * <p>The allowed list is configured via {@code mercadopago.allowed-ips} and supports both plain IP
 * addresses (e.g. {@code 127.0.0.1}) and CIDR notation (e.g. {@code 18.231.0.0/16}).
 *
 * <p><strong>Note on X-Forwarded-For:</strong> When the application runs behind a trusted reverse
 * proxy or load balancer that sets this header, the resolved IP is the original client address.
 * Ensure your proxy overwrites this header to prevent spoofing.
 */
@Component
@Slf4j
public class MercadoPagoIpValidator {

    private final List<String> allowedCidrs;

    public MercadoPagoIpValidator(
            @Value("#{'${mercadopago.allowed-ips}'.split(',')}") List<String> allowedCidrs) {
        this.allowedCidrs = allowedCidrs;
    }

    public boolean isAllowed(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return false;
        }
        try {
            InetAddress clientAddr = InetAddress.getByName(clientIp.trim());
            for (String entry : allowedCidrs) {
                String cidr = entry.trim();
                if (!cidr.isEmpty() && matchesCidr(clientAddr, cidr)) {
                    return true;
                }
            }
        } catch (UnknownHostException e) {
            log.warn("Could not resolve client IP '{}' for allowlist check.", clientIp);
        }
        return false;
    }

    private boolean matchesCidr(InetAddress clientAddr, String cidr) {
        try {
            if (!cidr.contains("/")) {
                return InetAddress.getByName(cidr).equals(clientAddr);
            }
            String[] parts = cidr.split("/", 2);
            int prefixLen = Integer.parseInt(parts[1]);
            InetAddress networkAddr = InetAddress.getByName(parts[0]);

            byte[] clientBytes = clientAddr.getAddress();
            byte[] networkBytes = networkAddr.getAddress();

            if (clientBytes.length != networkBytes.length) {
                return false;
            }

            int fullBytes = prefixLen / 8;
            int remainBits = prefixLen % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (clientBytes[i] != networkBytes[i]) {
                    return false;
                }
            }
            if (remainBits > 0) {
                int mask = (0xFF << (8 - remainBits)) & 0xFF;
                if ((clientBytes[fullBytes] & mask) != (networkBytes[fullBytes] & mask)) {
                    return false;
                }
            }
            return true;

        } catch (UnknownHostException | NumberFormatException e) {
            log.warn("Invalid CIDR entry in allowlist: '{}'", cidr);
            return false;
        }
    }
}

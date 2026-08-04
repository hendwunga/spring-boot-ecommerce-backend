package com.hendro.ecommerce.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hendro.ecommerce.entity.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtService {

    private static final String ALGORITHM = "HS256";
    private static final long DEFAULT_EXPIRY_MILLIS = 60 * 60 * 1000L; // 1 hour

    private final SecretKeySpec signingKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.signingKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public String generateToken(AppUser user) {
        return generateToken(user, DEFAULT_EXPIRY_MILLIS);
    }

    public String generateToken(AppUser user, long expiryMillis) {
        long now = System.currentTimeMillis();

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", ALGORITHM);
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.getEmail());
        payload.put("email", user.getEmail());
        payload.put("name", (user.getFirstName() + " " + user.getLastName()).trim());
        payload.put("iat", now / 1000);
        payload.put("exp", (now + expiryMillis) / 1000);

        String signingInput = base64UrlEncode(toJson(header)) + "." + base64UrlEncode(toJson(payload));
        String signature = base64UrlEncode(sign(signingInput));

        return signingInput + "." + signature;
    }

    public String getEmailFromToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Token format is invalid");
        }

        String signingInput = parts[0] + "." + parts[1];

        if (!constantTimeEquals(sign(parts[0] + "." + parts[1]), base64UrlDecode(parts[2]))) {
            throw new IllegalArgumentException("Token signature is invalid");
        }

        JsonNode payload;
        try {
            payload = objectMapper.readTree(base64UrlDecode(parts[1]));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Token payload is invalid");
        }

        if (!payload.hasNonNull("exp")) {
            throw new IllegalArgumentException("Token has no expiration");
        }

        long expSeconds = payload.get("exp").asLong();
        if (expSeconds * 1000 < System.currentTimeMillis()) {
            throw new IllegalArgumentException("Token has expired");
        }

        if (!payload.hasNonNull("email")) {
            throw new IllegalArgumentException("Token has no email claim");
        }

        return payload.get("email").asText();
    }

    private byte[] sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign token", ex);
        }
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize token claims", ex);
        }
    }

    private String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private String base64UrlEncode(String data) {
        return base64UrlEncode(data.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] base64UrlDecode(String data) {
        return Base64.getUrlDecoder().decode(data);
    }

    private boolean constantTimeEquals(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a, b);
    }

}

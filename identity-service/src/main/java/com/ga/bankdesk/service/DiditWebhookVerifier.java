package com.ga.bankdesk.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

/**
 * checks whether a webhook received from Didit is authentic
 */

@Component
public class DiditWebhookVerifier{

    private static final long MAX_AGE_SECONDS = 300;
    private final String secret;

    public DiditWebhookVerifier(@Value("${app.kyc.didit.webhook-secret}") String secret) {
        this.secret = secret;
    }

    //true is the timestamp is new and the HMAC-SHA256 of the exact raw body matches the signature header
    public boolean isValid(byte[] rawBody, String signature, String timestamp) {
        if (secret.isBlank() || signature == null || timestamp == null) return false;
        try {
            long ts = Long.parseLong(timestamp.trim());
            if (ts > 100_000_000_000L) ts = ts / 1000;
            if (Math.abs(Instant.now().getEpochSecond() - ts) > MAX_AGE_SECONDS) return false;

            //creating the HMAC signature to check authenticity of data
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(rawBody));

            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}

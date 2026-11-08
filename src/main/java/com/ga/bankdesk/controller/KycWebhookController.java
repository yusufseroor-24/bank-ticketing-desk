package com.ga.bankdesk.controller;

import com.ga.bankdesk.service.DiditWebhookVerifier;
import com.ga.bankdesk.service.KycService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/kyc")
public class KycWebhookController {

    private final DiditWebhookVerifier verifier;
    private final KycService kycService;

    //takes the body as raw bytes, the signature is computed over the exact bytes Didit sent
    @PostMapping("/webhook")
    public ResponseEntity<Void> receive(@RequestBody byte[] rawBody,
                                        @RequestHeader(value = "X-Signature", required = false) String signature,
                                        @RequestHeader(value = "X-Timestamp", required = false) String timestamp) {
        if (!verifier.isValid(rawBody, signature, timestamp)) {
            log.warn("Rejected KYC webhook: invalid or missing signature (X-Signature present: {})", signature != null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, Object> payload;
        try {
            payload = JsonParserFactory.getJsonParser().parseMap(new String(rawBody, StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }

        Object sessionId = payload.get("session_id");
        Object status = payload.get("status");
        if (sessionId != null && status != null) {
            kycService.handleStatusUpdate(sessionId.toString(), status.toString());
        }
        return ResponseEntity.ok().build();
    }
}

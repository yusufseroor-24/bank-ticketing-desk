package com.ga.bankdesk.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

/**
 * Communicates with Didit for KYC and creates verification sessions for users
 */

@Component
public class DiditClient {

    public record DiditSession(String sessionId, String url){}

    //send HTTP request to Didit
    private final RestClient restClient;
    private final String apiKey;
    private final String workflowId;
    private final String callbackUrl;

    public DiditClient(@Value("${app.kyc.didit.base-url}") String baseUrl,
                       @Value("${app.kyc.didit.api-key}") String apiKey,
                       @Value("${app.kyc.didit.workflow-id}") String workflowId,
                       @Value("${app.kyc.callback-url}") String callbackUrl){
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.apiKey = apiKey;
        this.workflowId = workflowId;
        this.callbackUrl = callbackUrl;
    }

    //checks wether Didit is configured
    public boolean isConfigured(){
        return !apiKey.isBlank() && !workflowId.isBlank();
    }

    //Creates a hosted verification session
    //vendor_data matches the webhook back to the user
    public DiditSession createSession(Long userId) {
        //built as a plain string so the request has a fixed length and an exact JSON body
        String body = "{\"workflow_id\":\"" + esc(workflowId) + "\","
                + "\"vendor_data\":\"" + esc(String.valueOf(userId)) + "\","
                + "\"callback\":\"" + esc(callbackUrl) + "\"}";

        Map<String, Object> response = restClient.post()
                .uri("/v3/session/")
                .header("x-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        if (response == null || response.get("session_id") == null) {
            throw new IllegalStateException("Didit response had no session_id");
        }
        Object link = response.get("url") != null ? response.get("url") : response.get("verification_url");
        if (link == null) {
            throw new IllegalStateException("Didit response had no verification link, keys: " + response.keySet());
        }
        return new DiditSession(response.get("session_id").toString(), link.toString());
    }

    private static String esc(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

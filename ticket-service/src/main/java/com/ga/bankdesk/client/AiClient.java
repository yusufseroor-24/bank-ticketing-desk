package com.ga.bankdesk.client;

import com.ga.bankdesk.dto.*;
import com.ga.bankdesk.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * communicates with ai-service
 */

@Slf4j
@Component
public class AiClient {

    private final RestClient restClient;

    public AiClient(@Value("${ai.service.url}") String baseUrl,
                    @Value("${internal.api-key:}") String internalKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(40));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("X-Internal-Key", internalKey)
                .build();
    }

    public DraftResponse draft(DraftCall call) {
        return post("/internal/ai/draft-ticket", call, DraftResponse.class);
    }

    public TriageResponse triage(TriageCall call) {
        return post("/internal/ai/triage", call, TriageResponse.class);
    }

    public SummaryResponse summarize(SummaryCall call) {
        return post("/internal/ai/summarize", call, SummaryResponse.class);
    }

    private <T> T post(String path, Object body, Class<T> type) {
        try {
            return restClient.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(body)
                    .retrieve().body(type);
        } catch (Exception e) {
            log.error("ai-service call failed: {}", e.getMessage());
            throw new ExternalServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI is unavailable right now, please try again later");
        }
    }
}
package com.ga.bankdesk.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ga.bankdesk.exception.AiUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Talks to OpenAI
 */

@Slf4j
@Component
public class OpenAiClient {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String content, String refusal) {
    }

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final String reasoningEffort;

    public OpenAiClient(@Value("${openai.base-url}") String baseUrl,
                        @Value("${openai.api-key:}") String apiKey,
                        @Value("${openai.model:}") String model,
                        @Value("${openai.reasoning-effort:}") String reasoningEffort,
                        @Value("${openai.timeout-seconds:35}") int timeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.apiKey = apiKey;
        this.model = model;
        this.reasoningEffort = reasoningEffort;
    }

    public <T> T complete(String system, String user, String schemaName, Map<String, Object> schema, Class<T> type) {
        if (apiKey.isBlank() || model.isBlank()) {
            log.error("OPENAI_API_KEY or OPENAI_MODEL is not set");
            throw new AiUnavailableException("AI is not configured");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", user)));
        body.put("response_format", Map.of("type", "json_schema",
                "json_schema", Map.of("name", schemaName, "strict", true, "schema", schema)));
        if (!reasoningEffort.isBlank()) {
            body.put("reasoning_effort", reasoningEffort);
        }

        String raw;
        try {
            raw = restClient.post().uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mapper.writeValueAsString(body))
                    .retrieve().body(String.class);
        } catch (RestClientResponseException e) {
            String details = e.getResponseBodyAsString();
            log.error("OpenAI returned {}: {}", e.getStatusCode().value(),
                    details.length() > 300 ? details.substring(0, 300) : details);
            throw new AiUnavailableException("AI is unavailable right now, please try again later");
        } catch (Exception e) {
            log.error("OpenAI call failed: {}", e.getMessage());
            throw new AiUnavailableException("AI is unavailable right now, please try again later");
        }
        try {
            ChatResponse response = mapper.readValue(raw, ChatResponse.class);
            Message message = response.choices().get(0).message();
            if (message.refusal() != null && !message.refusal().isBlank()) {
                log.warn("OpenAI refused the request");
                throw new AiUnavailableException("AI could not help with this request");
            }
            return mapper.readValue(message.content(), type);
        } catch (AiUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Could not read the OpenAI answer: {}", e.getMessage());
            throw new AiUnavailableException("AI is unavailable right now, please try again later");
        }
    }
}

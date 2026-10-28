package com.ga.bankdesk.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * send VirusTotal the file attachment hash
 */

@Slf4j
@Component
public class VirusTotalClient {

    public record Stats(int malicious, int suspicious, int harmless, int undetected) {
        public int total() {
            return malicious + suspicious + harmless + undetected;
        }
    }

    private final RestClient restClient;
    private final String apiKey;

    public VirusTotalClient(@Value("${app.virustotal.api-key:}") String apiKey,
                            @Value("${app.virustotal.base-url:https://www.virustotal.com/api/v3}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.apiKey = apiKey;
    }

    // Empty = VirusTotal has never seen this hash
    public Optional<Stats> lookup(String sha256) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("VirusTotal API key is not configured");
        }
        try {
            Map<String, Object> body = restClient.get()
                    .uri("/files/{hash}", sha256)
                    .header("x-apikey", apiKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            return Optional.of(parse(body));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new IllegalStateException("VirusTotal rate limit reached, try again later");
        }
    }

    @SuppressWarnings("unchecked")
    static Stats parse(Map<String, Object> body) {
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        Map<String, Object> attributes = (Map<String, Object>) data.get("attributes");
        Map<String, Object> stats = (Map<String, Object>) attributes.get("last_analysis_stats");
        return new Stats(intOf(stats.get("malicious")), intOf(stats.get("suspicious")),
                intOf(stats.get("harmless")), intOf(stats.get("undetected")));
    }

    private static int intOf(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }
}

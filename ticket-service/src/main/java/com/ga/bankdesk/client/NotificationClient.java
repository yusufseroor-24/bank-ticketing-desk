package com.ga.bankdesk.client;

import com.ga.bankdesk.dto.EmailRequest;
import com.ga.bankdesk.dto.LiveNotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Calls notification-service to send emails and live (SSE) messages.
 */

@Slf4j
@Component
public class NotificationClient {

    private final RestClient restClient;

    public NotificationClient(@Value("${notification.service.url}") String baseUrl,
                              @Value("${internal.api-key:}") String internalKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("X-Internal-Key", internalKey)
                .build();
    }

    @Async("notificationExecutor")
    public void email(EmailRequest request) {
        try {
            restClient.post().uri("/internal/notifications/email").body(request).retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.error("Could not send email request '{}' to notification-service: {}", request.type(), e.getMessage());
        }
    }

    @Async("notificationExecutor")
    public void live(LiveNotificationRequest request) {
        try {
            restClient.post().uri("/internal/notifications/live").body(request).retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.error("Could not send live notification '{}' to notification-service: {}", request.eventType(), e.getMessage());
        }
    }
}

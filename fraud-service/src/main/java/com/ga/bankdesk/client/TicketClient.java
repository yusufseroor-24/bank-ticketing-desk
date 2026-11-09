package com.ga.bankdesk.client;

import com.ga.bankdesk.dto.SystemTicketRequest;
import com.ga.bankdesk.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

/**
 * Calls ticket-service  to open the investigation ticket of a fraud alert.
 */

@Slf4j
@Component
public class TicketClient {

    private final RestClient restClient;

    public TicketClient(@Value("${ticket.service.url}") String baseUrl,
                        @Value("${internal.api-key:}") String internalKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("X-Internal-Key", internalKey)
                .build();
    }

    //returns the id of the new ticket
    public Long createSystemTicket(SystemTicketRequest request) {
        try {
            Map<String, Object> response = restClient.post()
                    .uri("/internal/tickets/system")
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            if (response == null || response.get("ticketId") == null) {
                throw new IllegalStateException("ticket-service response had no ticketId");
            }
            return ((Number) response.get("ticketId")).longValue();
        } catch (Exception e) {
            log.error("ticket-service call failed: {}", e.getMessage());
            throw new ExternalServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Ticket service is unavailable, please try again");
        }
    }
}

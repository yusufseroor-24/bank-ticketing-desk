package com.ga.bankdesk.client;

import com.ga.bankdesk.dto.UserSummary;
import com.ga.bankdesk.enums.Role;
import com.ga.bankdesk.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Calls identity-service to get user data that this service no longer stores.
 */
@Slf4j
@Component
public class IdentityClient {

    private final RestClient restClient;

    public IdentityClient(@Value("${identity.service.url}") String baseUrl,
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

    //empty if the user doesn't exist
    public Optional<UserSummary> findUser(Long id) {
        try {
            return Optional.ofNullable(restClient.get().uri("/internal/users/{id}", id)
                    .retrieve().body(UserSummary.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (Exception e) {
            throw unavailable(e);
        }
    }

    public Optional<UserSummary> findByEmail(String email) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri(u -> u.path("/internal/users/by-email").queryParam("email", "{email}").build(email))
                    .retrieve().body(UserSummary.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (Exception e) {
            throw unavailable(e);
        }
    }

    //several users in one call
    public List<UserSummary> getUsers(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        String csv = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        try {
            List<UserSummary> users = restClient.get()
                    .uri(u -> u.path("/internal/users").queryParam("ids", csv).build())
                    .retrieve().body(new ParameterizedTypeReference<List<UserSummary>>() {});
            return users == null ? List.of() : users;
        } catch (Exception e) {
            throw unavailable(e);
        }
    }

    public List<UserSummary> getUsersByRole(Role role) {
        try {
            List<UserSummary> users = restClient.get()
                    .uri(u -> u.path("/internal/users").queryParam("role", role.name()).build())
                    .retrieve().body(new ParameterizedTypeReference<List<UserSummary>>() {});
            return users == null ? List.of() : users;
        } catch (Exception e) {
            throw unavailable(e);
        }
    }

    private ExternalServiceException unavailable(Exception e) {
        log.error("identity-service call failed: {}", e.getMessage());
        return new ExternalServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Identity service is unavailable, please try again");
    }
}

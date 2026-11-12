package com.ga.bankdesk.cucumber;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class FakeIdentityService {

    //scenarios set this to true to simulate identity-service being down
    public static volatile boolean down = false;

    private static final Map<Long, String> USERS = new LinkedHashMap<>();
    private static HttpServer server;

    static {
        USERS.put(1L, user(1, "verified@bankdesk.test", "CUSTOMER", "APPROVED"));
        USERS.put(2L, user(2, "unverified@bankdesk.test", "CUSTOMER", "NOT_STARTED"));
        for (long id = 101; id <= 105; id++) {
            USERS.put(id, user(id, "agent" + id + "@bankdesk.test", "AGENT", "NOT_STARTED"));
        }
        USERS.put(201L, user(201, "agent201@bankdesk.test", "AGENT", "NOT_STARTED"));
    }

    private static String user(long id, String email, String role, String kyc) {
        return "{\"id\":%d,\"email\":\"%s\",\"fullName\":\"%s\",\"role\":\"%s\",\"status\":\"ACTIVE\",\"kycStatus\":\"%s\"}"
                .formatted(id, email, email, role, kyc);
    }

    public static synchronized String start() {
        if (server == null) {
            try {
                server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            server.createContext("/internal/users", FakeIdentityService::handle);
            server.start();
        }
        return "http://localhost:" + server.getAddress().getPort();
    }

    private static void handle(HttpExchange ex) {
        try {
            if (down) {
                reply(ex, 500, "{}");
                return;
            }
            String path = ex.getRequestURI().getPath();
            Map<String, String> query = query(ex.getRequestURI().getRawQuery());

            if (path.equals("/internal/users")) {
                if (query.containsKey("ids")) {
                    String list = Arrays.stream(query.get("ids").split(","))
                            .map(s -> USERS.get(Long.parseLong(s.trim())))
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining(","));
                    reply(ex, 200, "[" + list + "]");
                } else {
                    reply(ex, 200, "[]");
                }
            } else if (path.equals("/internal/users/by-email")) {
                String email = query.get("email");
                String found = USERS.values().stream().filter(u -> u.contains("\"" + email + "\"")).findFirst().orElse(null);
                reply(ex, found == null ? 404 : 200, found == null ? "{}" : found);
            } else {
                long id = Long.parseLong(path.substring("/internal/users/".length()));
                String found = USERS.get(id);
                reply(ex, found == null ? 404 : 200, found == null ? "{}" : found);
            }
        } catch (Exception e) {
            try { reply(ex, 500, "{}"); } catch (Exception ignored) { }
        }
    }

    private static Map<String, String> query(String raw) {
        Map<String, String> map = new LinkedHashMap<>();
        if (raw == null) return map;
        for (String pair : raw.split("&")) {
            String[] kv = pair.split("=", 2);
            map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return map;
    }

    private static void reply(HttpExchange ex, int status, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (var out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }
}
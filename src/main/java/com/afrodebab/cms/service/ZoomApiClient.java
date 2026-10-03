package com.afrodebab.cms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

/** Thin HTTP client for Zoom OAuth and the meetings API, plain java.net.http like {@link GoogleApiClient}. */
@Component
public class ZoomApiClient {
    public static final String API = "https://api.zoom.us/v2";
    private static final String OAUTH = "https://zoom.us/oauth";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String clientId;
    private final String clientSecret;

    public ZoomApiClient(ObjectMapper objectMapper,
                         @Value("${app.zoom.client-id:}") String clientId,
                         @Value("${app.zoom.client-secret:}") String clientSecret) {
        this.objectMapper = objectMapper;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public record Tokens(String accessToken, String refreshToken) {}

    public boolean isConfigured() {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }

    public Tokens exchangeCode(String code, String redirectUri) {
        return tokens(Map.of("grant_type", "authorization_code", "code", code, "redirect_uri", redirectUri));
    }

    /** Zoom returns a new refresh token every time; the caller must store it. */
    public Tokens refresh(String refreshToken) {
        return tokens(Map.of("grant_type", "refresh_token", "refresh_token", refreshToken));
    }

    /** Best effort: the local disconnect goes ahead even if Zoom can't be reached. */
    public void revoke(String token) {
        try {
            postForm(OAUTH + "/revoke", Map.of("token", token));
        } catch (RuntimeException ignored) {
            // Already revoked or expired; nothing left to undo on Zoom's side.
        }
    }

    public String userEmail(String accessToken) {
        return request("GET", API + "/users/me", accessToken, null).path("email").asText(null);
    }

    /** JSON request with a bearer token; {@code body} may be null. Returns an empty node for 204. */
    public JsonNode request(String method, String url, String accessToken, Object body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json");
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            try {
                builder.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to serialize Zoom request", e);
            }
        }
        return send(builder);
    }

    private Tokens tokens(Map<String, String> form) {
        JsonNode res = postForm(OAUTH + "/token", form);
        return new Tokens(res.path("access_token").asText(null), res.path("refresh_token").asText(null));
    }

    private JsonNode postForm(String url, Map<String, String> form) {
        String body = form.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        String basic = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        return send(HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Basic " + basic)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)));
    }

    private JsonNode send(HttpRequest.Builder request) {
        try {
            return read(httpClient.send(request.timeout(Duration.ofSeconds(30)).build(), HttpResponse.BodyHandlers.ofString()));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Zoom request interrupted", e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to reach Zoom", e);
        }
    }

    private JsonNode read(HttpResponse<String> res) throws Exception {
        String text = res.body() == null ? "" : res.body();
        JsonNode json = text.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(text);
        if (res.statusCode() >= 200 && res.statusCode() < 300) return json;
        String message = json.path("message").asText(json.path("reason").asText(json.path("error").asText("")));
        // Zoom's 401/403 means our token is bad, not the caller's session; don't leak it as a 401 to the browser.
        HttpStatus status = res.statusCode() == 404 ? HttpStatus.NOT_FOUND : HttpStatus.BAD_GATEWAY;
        throw new ResponseStatusException(status, "Zoom API error (" + res.statusCode() + "): " + message);
    }
}

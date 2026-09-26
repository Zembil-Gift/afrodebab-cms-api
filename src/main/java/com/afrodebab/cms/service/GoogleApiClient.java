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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Thin HTTP client for Google OAuth and the Calendar / Sheets REST APIs, using plain
 * java.net.http like the GitHub and Trello integrations (no Google SDK). Failures surface as
 * {@link ResponseStatusException} with Google's own error message.
 */
@Component
public class GoogleApiClient {
    public static final String SCOPE_CALENDAR = "https://www.googleapis.com/auth/calendar.events";
    public static final String SCOPE_SHEETS = "https://www.googleapis.com/auth/drive.file";

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String clientId;
    private final String clientSecret;

    public GoogleApiClient(ObjectMapper objectMapper,
                           @Value("${app.google.client-id:}") String clientId,
                           @Value("${app.google.client-secret:}") String clientSecret) {
        this.objectMapper = objectMapper;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public record Tokens(String accessToken, String refreshToken, String scope) {}

    public boolean isConfigured() {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }

    public Tokens exchangeCode(String code, String redirectUri) {
        JsonNode res = postForm(TOKEN_URL, Map.of(
                "code", code,
                "client_id", clientId,
                "client_secret", clientSecret,
                "redirect_uri", redirectUri,
                "grant_type", "authorization_code"));
        return new Tokens(res.path("access_token").asText(null), res.path("refresh_token").asText(null),
                res.path("scope").asText(""));
    }

    public String refreshAccessToken(String refreshToken) {
        return postForm(TOKEN_URL, Map.of(
                "refresh_token", refreshToken,
                "client_id", clientId,
                "client_secret", clientSecret,
                "grant_type", "refresh_token")).path("access_token").asText(null);
    }

    /** Best effort: the local disconnect goes ahead even if Google can't be reached. */
    public void revoke(String token) {
        try {
            postForm("https://oauth2.googleapis.com/revoke", Map.of("token", token));
        } catch (RuntimeException ignored) {
            // Already revoked or expired; nothing left to undo on Google's side.
        }
    }

    public String userEmail(String accessToken) {
        return request("GET", "https://openidconnect.googleapis.com/v1/userinfo", accessToken, null)
                .path("email").asText(null);
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
                throw new IllegalStateException("Failed to serialize Google request", e);
            }
        }
        return send(builder);
    }

    private JsonNode postForm(String url, Map<String, String> form) {
        String body = form.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return send(HttpRequest.newBuilder(URI.create(url))
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
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google request interrupted", e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to reach Google", e);
        }
    }

    private JsonNode read(HttpResponse<String> res) throws Exception {
        String text = res.body() == null ? "" : res.body();
        JsonNode json = text.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(text);
        if (res.statusCode() >= 200 && res.statusCode() < 300) return json;
        String message = json.path("error").isObject()
                ? json.path("error").path("message").asText("")
                : json.path("error_description").asText(json.path("error").asText(""));
        // Google's 401/403 means our token is bad, not the caller's session; don't leak it as a 401 to the browser.
        HttpStatus status = res.statusCode() == 404 ? HttpStatus.NOT_FOUND : HttpStatus.BAD_GATEWAY;
        throw new ResponseStatusException(status, "Google API error (" + res.statusCode() + "): " + message);
    }
}

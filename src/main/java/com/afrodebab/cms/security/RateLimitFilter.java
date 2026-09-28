package com.afrodebab.cms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Per-IP limits on the anonymous POST endpoints, so password guessing and application spam are cut off
 * before they cost Gemini calls and emails.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private record Rule(String name, Pattern path, int limit, Duration window) {}

    private record Window(long resetAtMillis, AtomicInteger count) {}

    private static final List<Rule> RULES = List.of(
            new Rule("auth", Pattern.compile("^/(admin|manager|vice-manager|employee)/auth/.+"), 10, Duration.ofMinutes(1)),
            new Rule("signup", Pattern.compile("^/signup(/otp)?$"), 5, Duration.ofMinutes(10)),
            new Rule("apply", Pattern.compile("^/public/[^/]+/jobs/[^/]+/apply(/form)?$"), 20, Duration.ofHours(1)),
            // A branch kiosk clocks in everyone from one IP, so this limit only stops attendance-key guessing.
            new Rule("attendance", Pattern.compile("^/employee/me/(clock-in|clock-out|lunch-break-in|lunch-break-out)$"),
                    300, Duration.ofMinutes(1)));

    // ponytail: in-memory and per instance; move to Redis/Bucket4j if the API ever runs more than one replica.
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Rule rule = "POST".equals(request.getMethod()) ? ruleFor(request.getRequestURI()) : null;
        if (rule == null) {
            chain.doFilter(request, response);
            return;
        }

        long now = System.currentTimeMillis();
        Window window = windows.compute(rule.name() + "|" + clientIp(request), (key, old) ->
                old == null || old.resetAtMillis() <= now
                        ? new Window(now + rule.window().toMillis(), new AtomicInteger())
                        : old);
        if (window.count().incrementAndGet() > rule.limit()) {
            reject(request, response, (window.resetAtMillis() - now + 999) / 1000);
            return;
        }
        chain.doFilter(request, response);
    }

    @Scheduled(fixedRate = 600_000)
    public void evictExpired() {
        long now = System.currentTimeMillis();
        windows.values().removeIf(w -> w.resetAtMillis() <= now);
    }

    private static Rule ruleFor(String path) {
        return RULES.stream().filter(r -> r.path().matcher(path).matches()).findFirst().orElse(null);
    }

    // Production is reachable only through Caddy, which sets X-Forwarded-For to the real client;
    // the last entry is the one our proxy added, so a client can't spoof it by sending its own header.
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) return request.getRemoteAddr();
        String[] hops = forwarded.split(",");
        return hops[hops.length - 1].trim();
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, long retryAfterSeconds)
            throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("error", HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
        body.put("message", "Too many requests, please try again in " + retryAfterSeconds + " seconds");
        body.put("path", request.getRequestURI());

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

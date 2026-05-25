package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.TelegramSupportReportResponse;
import com.afrodebab.cms.dto.TelegramSupportSummaryAverages;
import com.afrodebab.cms.dto.TelegramSupportSummaryResponse;
import com.afrodebab.cms.dto.TelegramSupportSummaryTotals;
import com.afrodebab.cms.dto.TelegramSupportTicketsPagination;
import com.afrodebab.cms.dto.TelegramSupportTicketsResponse;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Employee;
import com.afrodebab.cms.jpa.repository.EmployeeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TelegramSupportTrackerService {
    private static final Logger log = LoggerFactory.getLogger(TelegramSupportTrackerService.class);

    private final EmployeeRepository employeeRepo;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${BOT_METRICS_BASE_URL:}")
    private String botMetricsBaseUrl;

    @Value("${BOT_METRICS_SECRET:}")
    private String botMetricsSecret;

    @Autowired
    public TelegramSupportTrackerService(EmployeeRepository employeeRepo,
                                         ObjectMapper objectMapper) {
        this(employeeRepo, objectMapper, HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    public TelegramSupportTrackerService(EmployeeRepository employeeRepo,
                                         ObjectMapper objectMapper,
                                         HttpClient httpClient) {
        this.employeeRepo = employeeRepo;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Transactional(readOnly = true)
    public TelegramSupportSummaryResponse getSummary(String adminUsername,
                                                     String typeGroup,
                                                     String from,
                                                     String to) {
        return fetchSummaryOrFallback(adminUsername, typeGroup, from, to);
    }

    @Transactional(readOnly = true)
    public TelegramSupportSummaryResponse getSummaryByEmail(String email,
                                                            String typeGroup,
                                                            String from,
                                                            String to) {
        Employee employee = employeeRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Employee not found with email: " + email));
        String telegramUsername = normalizeTelegramUsername(employee.getTelegramUsername());
        if (telegramUsername == null) {
            throw new BadRequestException("Employee telegramUsername is not set");
        }
        return fetchSummaryOrFallback(telegramUsername, typeGroup, from, to);
    }

    @Transactional(readOnly = true)
    public TelegramSupportTicketsResponse getTickets(String adminUsername,
                                                     String status,
                                                     String typeGroup,
                                                     String from,
                                                     String to,
                                                     Integer page,
                                                     Integer pageSize) {
        return fetchTicketsOrFallback(adminUsername, status, typeGroup, from, to, page, pageSize);
    }

    @Transactional(readOnly = true)
    public TelegramSupportTicketsResponse getTicketsByEmail(String email,
                                                            String status,
                                                            String typeGroup,
                                                            String from,
                                                            String to,
                                                            Integer page,
                                                            Integer pageSize) {
        Employee employee = employeeRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Employee not found with email: " + email));
        String telegramUsername = normalizeTelegramUsername(employee.getTelegramUsername());
        if (telegramUsername == null) {
            throw new BadRequestException("Employee telegramUsername is not set");
        }
        return fetchTicketsOrFallback(telegramUsername, status, typeGroup, from, to, page, pageSize);
    }

    @Transactional(readOnly = true)
    public TelegramSupportReportResponse getEmployeeReport(Long employeeId,
                                                           String status,
                                                           String typeGroup,
                                                           String from,
                                                           String to) {
        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found with ID: " + employeeId));
        String telegramUsername = normalizeTelegramUsername(employee.getTelegramUsername());
        if (telegramUsername == null) {
            throw new BadRequestException("Employee telegramUsername is not set");
        }

        TelegramSupportSummaryResponse summary = fetchSummaryOrFallback(telegramUsername, typeGroup, from, to);
        TelegramSupportTicketsResponse tickets = fetchTicketsOrFallback(telegramUsername, status, typeGroup, from, to, 1, 10);

        return new TelegramSupportReportResponse(
                employee.getId(),
                employee.getName(),
                telegramUsername,
                summary.totals(),
                summary.averages(),
                summary.countsByIssueType(),
                tickets.tickets()
        );
    }

    @Transactional(readOnly = true)
    public TelegramSupportReportResponse getEmployeeReportByEmail(String email,
                                                                  String status,
                                                                  String typeGroup,
                                                                  String from,
                                                                  String to) {
        Employee employee = employeeRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Employee not found with email: " + email));
        return getEmployeeReport(employee.getId(), status, typeGroup, from, to);
    }

    private TelegramSupportSummaryResponse fetchSummaryOrFallback(String adminUsername,
                                                                   String typeGroup,
                                                                   String from,
                                                                   String to) {
        try {
            return fetchSummary(adminUsername, typeGroup, from, to);
        } catch (Exception e) {
            log.error("Failed to fetch telegram support summary for {}: {}", adminUsername, e.getMessage());
            return new TelegramSupportSummaryResponse(
                    null,
                    new TelegramSupportSummaryTotals(0, 0, 0, 0),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    new TelegramSupportSummaryAverages(null, null)
            );
        }
    }

    private TelegramSupportSummaryResponse fetchSummary(String adminUsername,
                                                        String typeGroup,
                                                        String from,
                                                        String to) {
        ensureConfigured();
        Map<String, String> params = new LinkedHashMap<>();
        putIfPresent(params, "adminUsername", normalizeTelegramUsername(adminUsername));
        putIfPresent(params, "typeGroup", typeGroup);
        putIfPresent(params, "from", from);
        putIfPresent(params, "to", to);

        String url = buildUrl("/api/bot/admin/metrics/summary", params);
        return getForObject(url, TelegramSupportSummaryResponse.class);
    }

    private TelegramSupportTicketsResponse fetchTicketsOrFallback(String adminUsername,
                                                                   String status,
                                                                   String typeGroup,
                                                                   String from,
                                                                   String to,
                                                                   Integer page,
                                                                   Integer pageSize) {
        try {
            return fetchTickets(adminUsername, status, typeGroup, from, to, page, pageSize);
        } catch (Exception e) {
            log.error("Failed to fetch telegram support tickets for {}: {}", adminUsername, e.getMessage());
            return new TelegramSupportTicketsResponse(
                    null,
                    new TelegramSupportTicketsPagination(0, 0, 0, 0),
                    Collections.emptyList()
            );
        }
    }

    private TelegramSupportTicketsResponse fetchTickets(String adminUsername,
                                                        String status,
                                                        String typeGroup,
                                                        String from,
                                                        String to,
                                                        Integer page,
                                                        Integer pageSize) {
        ensureConfigured();
        Map<String, String> params = new LinkedHashMap<>();
        putIfPresent(params, "adminUsername", normalizeTelegramUsername(adminUsername));
        putIfPresent(params, "status", status);
        putIfPresent(params, "typeGroup", typeGroup);
        putIfPresent(params, "from", from);
        putIfPresent(params, "to", to);
        if (page != null) {
            params.put("page", String.valueOf(page));
        }
        if (pageSize != null) {
            params.put("pageSize", String.valueOf(pageSize));
        }

        String url = buildUrl("/api/bot/admin/metrics/tickets", params);
        return getForObject(url, TelegramSupportTicketsResponse.class);
    }

    private <T> T getForObject(String url, Class<T> responseType) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Bot-Metrics-Secret", botMetricsSecret)
                    .header("Accept", "application/json")
                    .header("User-Agent", "AfroDebab-CMS-API")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Telegram bot metrics API returned non-200 status: {} - {}", response.statusCode(), response.body());
                throw new RuntimeException("Telegram bot metrics API error: " + response.statusCode());
            }
            return objectMapper.readValue(response.body(), responseType);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Telegram bot metrics API error", e);
        }
    }

    private void ensureConfigured() {
        if (botMetricsBaseUrl == null || botMetricsBaseUrl.isBlank()
                || botMetricsSecret == null || botMetricsSecret.isBlank()) {
            throw new BadRequestException("BOT_METRICS_BASE_URL or BOT_METRICS_SECRET is not configured");
        }
    }

    private String buildUrl(String path, Map<String, String> params) {
        String base = botMetricsBaseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        StringBuilder url = new StringBuilder(base).append(path);
        if (!params.isEmpty()) {
            url.append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!first) {
                    url.append("&");
                }
                first = false;
                url.append(encode(entry.getKey()))
                        .append("=")
                        .append(encode(entry.getValue()));
            }
        }
        return url.toString();
    }

    private void putIfPresent(Map<String, String> params, String key, String value) {
        if (value != null && !value.isBlank()) {
            params.put(key, value);
        }
    }

    private String normalizeTelegramUsername(String username) {
        if (username == null) {
            return null;
        }
        String normalized = username.trim();
        if (normalized.startsWith("@")) {
            normalized = normalized.substring(1);
        }
        return normalized.isBlank() ? null : normalized;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}

package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.GoogleConnectionResponse;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.jpa.entity.Manager;
import com.afrodebab.cms.jpa.repository.ManagerRepository;
import com.afrodebab.cms.security.TokenCipher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Optional;

/**
 * Per-manager Google account link, used by interview scheduling (Calendar) and the Sheets
 * sync. The frontend runs Google's consent screen (offline access, incremental scopes) and
 * posts back the code; we exchange it server-side and keep only the encrypted refresh token.
 * Access tokens are short-lived and fetched on demand, never stored.
 */
@Service
public class ManagerGoogleConnectionService {

    private final ManagerRepository managerRepo;
    private final TokenCipher cipher;
    private final GoogleApiClient google;

    public ManagerGoogleConnectionService(ManagerRepository managerRepo, TokenCipher cipher, GoogleApiClient google) {
        this.managerRepo = managerRepo;
        this.cipher = cipher;
        this.google = google;
    }

    @Transactional
    public GoogleConnectionResponse connect(String code, String redirectUri) {
        if (!google.isConfigured() || !cipher.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Google integration is not configured (GOOGLE_CLIENT_ID/SECRET or TOKEN_ENCRYPTION_KEY missing)");
        }
        GoogleApiClient.Tokens tokens = google.exchangeCode(code, redirectUri);
        if (tokens.accessToken() == null) throw new BadRequestException("Invalid Google authorization code");

        Manager manager = currentManager();
        if (tokens.refreshToken() != null) {
            manager.setGoogleRefreshToken(cipher.encrypt(tokens.refreshToken()));
        } else if (manager.getGoogleRefreshToken() == null) {
            throw new BadRequestException("Google did not grant offline access; please connect again");
        }
        manager.setGoogleEmail(google.userEmail(tokens.accessToken()));
        manager.setGoogleScopes(tokens.scope());
        return toResponse(managerRepo.save(manager));
    }

    @Transactional(readOnly = true)
    public GoogleConnectionResponse status() {
        return toResponse(currentManager());
    }

    @Transactional
    public void disconnect() {
        Manager manager = currentManager();
        if (manager.getGoogleRefreshToken() != null) google.revoke(cipher.decrypt(manager.getGoogleRefreshToken()));
        manager.setGoogleRefreshToken(null);
        manager.setGoogleEmail(null);
        manager.setGoogleScopes(null);
        managerRepo.save(manager);
    }

    /** A fresh access token when the manager connected Google with {@code scope}; empty otherwise. */
    public Optional<String> accessToken(Manager manager, String scope) {
        if (!hasScope(manager, scope) || !google.isConfigured()) return Optional.empty();
        return Optional.ofNullable(google.refreshAccessToken(cipher.decrypt(manager.getGoogleRefreshToken())));
    }

    public static boolean hasScope(Manager manager, String scope) {
        return manager.getGoogleRefreshToken() != null && manager.getGoogleScopes() != null
                && Arrays.asList(manager.getGoogleScopes().split(" ")).contains(scope);
    }

    private Manager currentManager() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return managerRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Manager not found"));
    }

    private GoogleConnectionResponse toResponse(Manager manager) {
        return new GoogleConnectionResponse(
                google.isConfigured(),
                manager.getGoogleRefreshToken() != null,
                manager.getGoogleEmail(),
                hasScope(manager, GoogleApiClient.SCOPE_CALENDAR),
                hasScope(manager, GoogleApiClient.SCOPE_SHEETS));
    }
}

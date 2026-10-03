package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.ZoomConnectionResponse;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.jpa.entity.Manager;
import com.afrodebab.cms.jpa.repository.ManagerRepository;
import com.afrodebab.cms.security.TokenCipher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Per-manager Zoom account link, so interview meetings are hosted on the organization's own Zoom.
 * Same shape as {@link ManagerGoogleConnectionService}: the frontend runs Zoom's consent screen and
 * posts back the code; we keep only the encrypted refresh token.
 */
@Service
public class ManagerZoomConnectionService {

    private final ManagerRepository managerRepo;
    private final TokenCipher cipher;
    private final ZoomApiClient zoom;

    public ManagerZoomConnectionService(ManagerRepository managerRepo, TokenCipher cipher, ZoomApiClient zoom) {
        this.managerRepo = managerRepo;
        this.cipher = cipher;
        this.zoom = zoom;
    }

    @Transactional
    public ZoomConnectionResponse connect(String code, String redirectUri) {
        if (!zoom.isConfigured() || !cipher.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Zoom integration is not configured (ZOOM_CLIENT_ID/SECRET or TOKEN_ENCRYPTION_KEY missing)");
        }
        ZoomApiClient.Tokens tokens = zoom.exchangeCode(code, redirectUri);
        if (tokens.accessToken() == null || tokens.refreshToken() == null) {
            throw new BadRequestException("Invalid Zoom authorization code");
        }
        Manager manager = currentManager();
        manager.setZoomRefreshToken(cipher.encrypt(tokens.refreshToken()));
        manager.setZoomEmail(zoom.userEmail(tokens.accessToken()));
        return toResponse(managerRepo.save(manager));
    }

    @Transactional(readOnly = true)
    public ZoomConnectionResponse status() {
        return toResponse(currentManager());
    }

    @Transactional
    public void disconnect() {
        Manager manager = currentManager();
        if (manager.getZoomRefreshToken() != null) zoom.revoke(cipher.decrypt(manager.getZoomRefreshToken()));
        manager.setZoomRefreshToken(null);
        manager.setZoomEmail(null);
        managerRepo.save(manager);
    }

    /**
     * A fresh access token for the manager's Zoom account. Zoom invalidates the old refresh token, so the
     * new one is committed in its own transaction: a rollback of the caller must not lose it.
     */
    // ponytail: two refreshes racing for one manager leave one stale token; serialize per manager if that shows up.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String accessToken(Long managerId) {
        Manager manager = managerRepo.findById(managerId)
                .orElseThrow(() -> new BadRequestException("Manager not found"));
        if (manager.getZoomRefreshToken() == null || !zoom.isConfigured()) {
            throw new BadRequestException("Connect Zoom in Integrations to create Zoom meetings");
        }
        ZoomApiClient.Tokens tokens = zoom.refresh(cipher.decrypt(manager.getZoomRefreshToken()));
        manager.setZoomRefreshToken(cipher.encrypt(tokens.refreshToken()));
        managerRepo.save(manager);
        return tokens.accessToken();
    }

    private Manager currentManager() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return managerRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Manager not found"));
    }

    private ZoomConnectionResponse toResponse(Manager manager) {
        return new ZoomConnectionResponse(zoom.isConfigured(), manager.getZoomRefreshToken() != null, manager.getZoomEmail());
    }
}

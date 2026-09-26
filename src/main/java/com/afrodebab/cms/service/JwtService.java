package com.afrodebab.cms.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expiresMinutes;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiresMinutes}") long expiresMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiresMinutes = expiresMinutes;
    }

    /**
     * @param orgId the caller's organization id, baked into the token so every request is
     *              scoped to their org. Null for platform admins (global, no org).
     */
    public String generateToken(String subjectEmail, String role, Long orgId) {
        Instant now = Instant.now();
        Instant exp = now.plus(expiresMinutes, ChronoUnit.MINUTES);

        var claims = new java.util.HashMap<String, Object>();
        claims.put("role", role);
        if (orgId != null) claims.put("orgId", orgId);

        return Jwts.builder()
                .subject(subjectEmail)
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public String extractSubject(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String extractRole(String token) {
        Object role = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role");
        return role == null ? null : role.toString();
    }

    /** The organization id baked into the token, or null for platform-admin tokens. */
    public Long extractOrgId(String token) {
        Object orgId = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("orgId");
        if (orgId == null) return null;
        return Long.valueOf(orgId.toString());
    }
}

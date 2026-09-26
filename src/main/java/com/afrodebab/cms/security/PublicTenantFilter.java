package com.afrodebab.cms.security;

import com.afrodebab.cms.jpa.entity.Organization;
import com.afrodebab.cms.jpa.repository.OrganizationRepository;
import com.afrodebab.cms.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Resolves the tenant for anonymous public endpoints from the URL:
 * {@code /public/{orgSlug}/...}. Sets {@link TenantContext} so the org's content is
 * scoped correctly even though there is no authenticated user. Unknown or suspended
 * orgs get a 404. Always clears the context after the request.
 */
@Component
public class PublicTenantFilter extends OncePerRequestFilter {

    private final OrganizationRepository organizationRepository;

    public PublicTenantFilter(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/public/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String slug = extractSlug(request.getRequestURI());
        if (slug == null || slug.isBlank()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Organization not specified");
            return;
        }

        Optional<Organization> org = organizationRepository.findBySlugIgnoreCase(slug);
        if (org.isEmpty() || !"ACTIVE".equals(org.get().getStatus())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Organization not found");
            return;
        }

        try {
            TenantContext.set(org.get().getId());
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /** Extracts {slug} from /public/{slug}/... */
    private String extractSlug(String uri) {
        String[] parts = uri.split("/");
        // ["", "public", "{slug}", ...]
        return parts.length >= 3 ? parts[2] : null;
    }
}

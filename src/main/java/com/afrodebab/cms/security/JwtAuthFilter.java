package com.afrodebab.cms.security;


import com.afrodebab.cms.service.AdminUserDetailsService;
import com.afrodebab.cms.service.EmployeeUserDetailsService;
import com.afrodebab.cms.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AdminUserDetailsService adminUserDetailsService;
    private final EmployeeUserDetailsService employeeUserDetailsService;

    public JwtAuthFilter(JwtService jwtService,
                         AdminUserDetailsService adminUserDetailsService,
                         EmployeeUserDetailsService employeeUserDetailsService) {
        this.jwtService = jwtService;
        this.adminUserDetailsService = adminUserDetailsService;
        this.employeeUserDetailsService = employeeUserDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean protectedPath = path.startsWith("/admin") || path.startsWith("/employee/me");
        // Skip JWT check for public routes + login + swagger
        return !protectedPath
                || path.startsWith("/admin/auth")
                || path.startsWith("/employee/auth")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/swagger-ui.html");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No token -> let Spring Security decide (it will block /admin/**)
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            String email = jwtService.extractSubject(token);
            String role = jwtService.extractRole(token);

            // Only set auth if not already set
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails user = switch (role) {
                    case "ADMIN" -> adminUserDetailsService.loadUserByUsername(email);
                    case "EMPLOYEE" -> employeeUserDetailsService.loadUserByUsername(email);
                    default -> throw new IllegalArgumentException("Unknown token role");
                };

                var authentication = new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities()
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (Exception ignored) {
            // Invalid token -> clear context so it will be treated as unauthenticated
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}


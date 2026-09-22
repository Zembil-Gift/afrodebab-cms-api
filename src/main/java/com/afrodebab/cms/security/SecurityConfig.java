package com.afrodebab.cms.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(java.util.List.of( "https://profile-test-brown.vercel.app","https://www.afrodebab.com", "http://localhost:3000", "https://afrodebab.vercel.app"));
        config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(java.util.List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // swagger
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // auth logins must be public
                        .requestMatchers("/admin/auth/**").permitAll()
                        .requestMatchers("/manager/auth/**").permitAll()
                        .requestMatchers("/vice-manager/auth/**").permitAll()
                        .requestMatchers("/employee/auth/**").permitAll()

                        // anonymous, org-scoped public content (blog/events/jobs/apply)
                        .requestMatchers("/public/**").permitAll()

                        // public self-serve "Start free" signup submission
                        .requestMatchers(HttpMethod.POST, "/signup", "/signup/otp").permitAll()

                        // attendance endpoints resolve the employee (and org) from the request body
                        .requestMatchers(
                                HttpMethod.POST,
                                "/employee/me/clock-in",
                                "/employee/me/clock-out",
                                "/employee/me/lunch-break-in",
                                "/employee/me/lunch-break-out"
                        ).permitAll()

                        // platform admin (global): organization CRUD
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // per-org management (formerly /admin/**)
                        .requestMatchers("/manager/**").hasRole("MANAGER")
                        // vice manager (sub-org scoped management)
                        .requestMatchers("/vice-manager/**").hasRole("VICE_MANAGER")
                        // employee self-service
                        .requestMatchers("/employee/me/**").hasRole("EMPLOYEE")

                        // remaining endpoints
                        .anyRequest().permitAll()
                )
                // IMPORTANT: add JWT filter into Spring Security chain
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }
}

package com.pcagrade.order.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * API Key Authentication Filter for server-to-server communication
 * Used for synchronization endpoints between Symfony and Spring Boot
 */
@Component
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    @Value("${api.sync.key:default-sync-key-please-change-in-production}")
    private String apiSyncKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        log.info("🔍 ApiKeyAuthenticationFilter executing for: {}", request.getRequestURI());

        // Only apply to /api/sync/** endpoints
        if (!request.getRequestURI().startsWith("/api/sync/")) {
            log.info("⏭️ Skipping API Key check - not a sync endpoint");
            filterChain.doFilter(request, response);
            return;
        }

        // Extract API Key from header
        String apiKey = request.getHeader("X-API-Key");

        log.info("🔍 Expected API Key: {}", apiSyncKey);
        log.info("🔍 Received API Key: {}", apiKey);
        log.info("🔍 Keys match: {}", apiKey != null && apiKey.equals(apiSyncKey));

        if (apiKey != null && apiKey.equals(apiSyncKey)) {
            // Valid API Key - create system authentication with admin privileges
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            "SYNC_SERVICE",
                            null,
                            List.of(
                                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                                    new SimpleGrantedAuthority("ROLE_MANAGER")
                            )
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("✅ API Key authentication successful for sync endpoint: {}", request.getRequestURI());
        } else {
            log.warn("⚠️ Invalid or missing API Key for sync endpoint: {}", request.getRequestURI());
            log.warn("⚠️ API Key was null: {}", apiKey == null);
        }

        filterChain.doFilter(request, response);
    }
}
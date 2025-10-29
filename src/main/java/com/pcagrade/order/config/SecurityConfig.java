package com.pcagrade.order.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security Configuration with JWT and API Key authentication
 * FIXED: No circular reference - only inject filters and UserDetailsService
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // ✅ ONLY inject these - NO PasswordEncoder, NO AuthenticationProvider
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final ApiKeyAuthenticationFilter apiKeyAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - no authentication required
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // ✅ Sync endpoints - protected by API Key (handled by ApiKeyAuthenticationFilter)
                        .requestMatchers("/api/sync/**").authenticated()

                        // Teams et Employees - BEFORE restrictive rules
                        .requestMatchers(HttpMethod.GET, "/api/teams/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/teams/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/teams/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/teams/**").hasAnyRole("ADMIN", "MANAGER")

                        .requestMatchers(HttpMethod.GET, "/api/v2/teams/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v2/teams/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/v2/teams/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v2/teams/**").hasAnyRole("ADMIN", "MANAGER")

                        .requestMatchers(HttpMethod.GET, "/api/employees/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/employees/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/employees/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/employees/**").hasAnyRole("ADMIN", "MANAGER")

                        // Admin-only endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyRole("ADMIN", "MANAGER")

                        // Manager endpoints
                        .requestMatchers("/api/planning/generate").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/employees/*/activate", "/api/employees/*/deactivate").hasAnyRole("ADMIN", "MANAGER")

                        // Role-specific work assignment access
                        .requestMatchers(HttpMethod.GET, "/api/planning/assignments").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/planning/assignments/*/status").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/planning/assignments/employee/*").authenticated()

                        // Grader-specific endpoints
                        .requestMatchers("/api/grading/**").hasAnyRole("ADMIN", "MANAGER", "GRADER")

                        // Authenticator-specific endpoints
                        .requestMatchers("/api/certification/**").hasAnyRole("ADMIN", "MANAGER", "AUTHENTICATOR")

                        // Scanner-specific endpoints
                        .requestMatchers("/api/scanning/**").hasAnyRole("ADMIN", "MANAGER", "SCANNER")

                        // Preparer-specific endpoints
                        .requestMatchers("/api/preparation/**").hasAnyRole("ADMIN", "MANAGER", "PREPARER")

                        // Read-only access for viewers
                        .requestMatchers(HttpMethod.GET, "/api/orders/**").hasAnyRole("ADMIN", "MANAGER", "VIEWER", "GRADER", "AUTHENTICATOR", "SCANNER", "PREPARER")
                        .requestMatchers(HttpMethod.GET, "/api/cards/**").hasAnyRole("ADMIN", "MANAGER", "VIEWER", "GRADER", "AUTHENTICATOR", "SCANNER", "PREPARER")

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // ✅ Direct method call - no injection needed
                .authenticationProvider(authenticationProvider())
                // ✅ API Key filter BEFORE JWT filter
                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Password encoder bean
     * Uses BCrypt hashing algorithm for secure password storage
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication manager bean
     * Required for login authentication
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }

    /**
     * Authentication provider bean
     * ✅ Calls passwordEncoder() directly - no injection
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());  // ✅ Direct method call
        return authProvider;
    }

    /**
     * CORS configuration
     * Allows requests from frontend applications
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:8080",
                "http://localhost:4200"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
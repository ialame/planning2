package com.pcagrade.order.controller;

import com.pcagrade.order.dto.AuthenticationRequest;
import com.pcagrade.order.dto.AuthenticationResponse;
import com.pcagrade.order.dto.RegisterRequest;
import com.pcagrade.order.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * Handles user login, registration, and token refresh
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Employee authentication and registration endpoints")
@CrossOrigin(origins = "*")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    /**
     * Register a new employee
     */
    @PostMapping("/register")
    @Operation(summary = "Register new employee", description = "Creates a new employee account with specified roles")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    /**
     * Authenticate employee and generate JWT token
     */
    @PostMapping("/login")
    @Operation(summary = "Employee login", description = "Authenticates employee and returns JWT token")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    /**
     * Refresh JWT token using refresh token
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT token", description = "Generates new access token using refresh token")
    public ResponseEntity<AuthenticationResponse> refresh(@RequestHeader("Authorization") String refreshToken) {
        if (refreshToken != null && refreshToken.startsWith("Bearer ")) {
            String token = refreshToken.substring(7);
            return ResponseEntity.ok(authenticationService.refreshToken(token));
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * Validate JWT token
     */
    @GetMapping("/validate")
    @Operation(summary = "Validate JWT token", description = "Checks if the provided JWT token is valid")
    public ResponseEntity<Boolean> validateToken(@RequestHeader("Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            boolean isValid = authenticationService.validateToken(token.substring(7));
            return ResponseEntity.ok(isValid);
        }
        return ResponseEntity.ok(false);
    }
}
package com.zuriagro.controller;

import com.zuriagro.dto.auth.JwtResponse;
import com.zuriagro.dto.auth.LoginRequest;
import com.zuriagro.dto.auth.RegisterRequest;
import com.zuriagro.dto.auth.UserResponse;
import com.zuriagro.security.SecurityUtils;
import com.zuriagro.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user login, registration, and identity")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue JWT bearer token")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse response = authService.authenticate(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user account (defaults to ROLE_FARMER)")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        UserResponse response = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile details")
    public ResponseEntity<UserResponse> getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        UserResponse response = authService.getCurrentUser(username);
        return ResponseEntity.ok(response);
    }
}

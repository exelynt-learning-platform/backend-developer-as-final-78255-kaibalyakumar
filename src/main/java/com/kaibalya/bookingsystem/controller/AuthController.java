package com.kaibalya.bookingsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.kaibalya.bookingsystem.dto.request.LoginRequest;
import com.kaibalya.bookingsystem.dto.response.LoginResponse;
import com.kaibalya.bookingsystem.service.AuthService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login and JWT issuance")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @SecurityRequirements // no auth required for login
    @Operation(summary = "Authenticate a user and receive a JWT access token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}

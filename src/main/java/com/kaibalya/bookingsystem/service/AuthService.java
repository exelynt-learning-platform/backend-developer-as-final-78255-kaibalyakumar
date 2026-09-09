package com.kaibalya.bookingsystem.service;


import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.kaibalya.bookingsystem.dto.request.LoginRequest;
import com.kaibalya.bookingsystem.dto.response.LoginResponse;
import com.kaibalya.bookingsystem.security.CustomUserDetails;
import com.kaibalya.bookingsystem.security.JwtUtil;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String role = userDetails.getUser().getRole().name();
        String token = jwtUtil.generateToken(userDetails, role);

        return LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .username(userDetails.getUsername())
                .role(role)
                .expiresInMs(jwtUtil.getExpirationMs())
                .build();
    }
}

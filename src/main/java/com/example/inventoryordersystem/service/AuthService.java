package com.example.inventoryordersystem.service;

import com.example.inventoryordersystem.config.JwtTokenProvider;
import com.example.inventoryordersystem.dto.request.LoginRequest;
import com.example.inventoryordersystem.dto.response.LoginResponse;
import com.example.inventoryordersystem.exception.AuthenticationFailedException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JwtTokenProvider tokenProvider;

    public AuthService(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    public LoginResponse login(LoginRequest request) {
        if ("admin".equals(request.username()) && "password".equals(request.password())) {
            String token = tokenProvider.generateToken(request.username(), "ROLE_ADMIN");
            return LoginResponse.of(token);
        }

        throw new AuthenticationFailedException("ユーザー名またはパスワードが正しくありません");
    }
}

package com.example.inventoryordersystem.dto.response;

public record LoginResponse(
        String accessToken,
        String tokenType
) {
    public static LoginResponse of(String accessToken) {
        return new LoginResponse(accessToken, "Bearer");
    }
}


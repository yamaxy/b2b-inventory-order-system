package com.example.inventoryordersystem.exception;

public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
}


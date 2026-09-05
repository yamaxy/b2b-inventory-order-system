package com.example.inventoryordersystem.exception;

public class ProductCodeAlreadyExistsException extends RuntimeException {
    public ProductCodeAlreadyExistsException(String message) {
        super(message);
    }
}

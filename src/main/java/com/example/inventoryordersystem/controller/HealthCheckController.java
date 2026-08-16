package com.example.inventoryordersystem.controller;

import com.example.inventoryordersystem.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthCheckController {

    // 1. 正常系確認用（200 OK）
    @GetMapping
    public Map<String, String> healthCheck() {
        return Map.of("status", "UP");
    }

    // 2. 404 エラーハンドリングテスト用
    @GetMapping("/test-not-found")
    public void testNotFound() {
        throw new ResourceNotFoundException("Test resource was not found");
    }

    // 3. 500 エラーハンドリングテスト用
    @GetMapping("/test-error")
    public void testError() {
        throw new RuntimeException("Test unexpected exception");
    }
}


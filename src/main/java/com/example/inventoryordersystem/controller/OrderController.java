package com.example.inventoryordersystem.controller;

import com.example.inventoryordersystem.dto.request.OrderCreateRequest;
import com.example.inventoryordersystem.dto.response.OrderResponse;
import com.example.inventoryordersystem.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderCreateRequest request,
            Authentication authentication
    ) {
        String currentUsername = authentication.getName();
        OrderResponse response = orderService.createOrder(request, currentUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

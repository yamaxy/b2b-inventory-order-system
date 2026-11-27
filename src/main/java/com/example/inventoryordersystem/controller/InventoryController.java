package com.example.inventoryordersystem.controller;

import com.example.inventoryordersystem.dto.request.InventoryAdjustmentRequest;
import com.example.inventoryordersystem.dto.response.InventoryAlertResponse;
import com.example.inventoryordersystem.dto.response.InventoryStatusResponse;
import com.example.inventoryordersystem.dto.response.StockHistoryResponse;
import com.example.inventoryordersystem.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<InventoryStatusResponse>> getInventoryList(
            @PageableDefault(sort = "id") Pageable pageable) {
        return ResponseEntity.ok(inventoryService.getInventoryList(pageable));
    }

    @GetMapping("/alerts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InventoryAlertResponse>> getInventoryAlerts() {
        return ResponseEntity.ok(inventoryService.getInventoryAlerts());
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<StockHistoryResponse> adjustStock(
            @Valid @RequestBody InventoryAdjustmentRequest request,
            Authentication authentication) {
        String currentUsername = authentication.getName();
        StockHistoryResponse response = inventoryService.adjustStock(request, currentUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

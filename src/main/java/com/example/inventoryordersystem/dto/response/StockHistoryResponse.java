package com.example.inventoryordersystem.dto.response;

import com.example.inventoryordersystem.entity.StockChangeType;
import com.example.inventoryordersystem.entity.StockHistory;

import java.time.LocalDateTime;

public record StockHistoryResponse(
        Long id,
        Long productId,
        String productCode,
        String productName,
        Long userId,
        StockChangeType changeType,
        Integer quantityChange,
        Integer currentStockAfter,
        String reason,
        LocalDateTime createdAt
) {
    public static StockHistoryResponse fromEntity(StockHistory history, Integer currentStockAfter) {
        Long userId = history.getUser() != null ? history.getUser().getId() : null;
        return new StockHistoryResponse(
                history.getId(),
                history.getProduct().getId(),
                history.getProduct().getProductCode(),
                history.getProduct().getName(),
                userId,
                history.getChangeType(),
                history.getQuantityChange(),
                currentStockAfter,
                history.getReason(),
                history.getCreatedAt()
        );
    }
}

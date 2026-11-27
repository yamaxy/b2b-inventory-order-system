package com.example.inventoryordersystem.dto.response;

import com.example.inventoryordersystem.entity.Product;

public record InventoryStatusResponse(
        Long productId,
        String productCode,
        String productName,
        Integer currentStock,
        Integer availableStock,
        Integer safetyStock,
        boolean belowSafetyStock,
        Integer version
) {
    public static InventoryStatusResponse fromEntity(Product product) {
        int current = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
        int safety = product.getSafetyStock() != null ? product.getSafetyStock() : 0;
        return new InventoryStatusResponse(
                product.getId(),
                product.getProductCode(),
                product.getName(),
                current,
                current,
                safety,
                current < safety,
                product.getVersion()
        );
    }
}

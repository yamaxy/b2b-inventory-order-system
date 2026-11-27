package com.example.inventoryordersystem.dto.response;

import com.example.inventoryordersystem.entity.Product;

public record InventoryAlertResponse(
        Long productId,
        String productCode,
        String productName,
        Integer currentStock,
        Integer safetyStock,
        Integer shortageQuantity
) {
    public static InventoryAlertResponse fromEntity(Product product) {
        int current = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
        int safety = product.getSafetyStock() != null ? product.getSafetyStock() : 0;
        int shortage = Math.max(0, safety - current);
        return new InventoryAlertResponse(
                product.getId(),
                product.getProductCode(),
                product.getName(),
                current,
                safety,
                shortage
        );
    }
}

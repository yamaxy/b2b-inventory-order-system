package com.example.inventoryordersystem.dto.request;

import com.example.inventoryordersystem.entity.StockChangeType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InventoryAdjustmentRequest(
        @NotNull(message = "商品IDは必須です")
        Long productId,

        @NotNull(message = "変動種別は必須です")
        StockChangeType changeType,

        @NotNull(message = "変動数は必須です")
        Integer quantityChange,

        @Size(max = 255, message = "変動理由は255文字以内で指定してください")
        String reason
) {
}

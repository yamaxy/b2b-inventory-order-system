package com.example.inventoryordersystem.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemRequest {

    @NotNull(message = "商品IDは必須です")
    private Long productId;

    @NotNull(message = "数量は必須です")
    @Min(value = 1, message = "数量は1以上を指定してください")
    private Integer quantity;
}


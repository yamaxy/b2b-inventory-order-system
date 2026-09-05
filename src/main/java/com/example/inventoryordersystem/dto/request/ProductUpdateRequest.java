package com.example.inventoryordersystem.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductUpdateRequest {

    @NotBlank(message = "商品名は必須です")
    @Size(max = 200, message = "商品名は200文字以内で指定してください")
    private String name;

    @NotNull(message = "価格は必須です")
    @DecimalMin(value = "0.0", message = "価格は0以上で指定してください")
    private BigDecimal price;

    @NotNull(message = "現在庫数は必須です")
    @Min(value = 0, message = "現在庫数は0以上で指定してください")
    private Integer currentStock;

    @NotNull(message = "安全在庫数は必須です")
    @Min(value = 0, message = "安全在庫数は0以上で指定してください")
    private Integer safetyStock;
}


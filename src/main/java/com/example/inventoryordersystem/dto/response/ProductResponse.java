package com.example.inventoryordersystem.dto.response;

import com.example.inventoryordersystem.entity.Product;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ProductResponse {

    private Long id;
    private String productCode;
    private String name;
    private BigDecimal price;
    private Integer currentStock;
    private Integer safetyStock;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponse fromEntity(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .productCode(product.getProductCode())
                .name(product.getName())
                .price(product.getPrice())
                .currentStock(product.getCurrentStock())
                .safetyStock(product.getSafetyStock())
                .version(product.getVersion())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}

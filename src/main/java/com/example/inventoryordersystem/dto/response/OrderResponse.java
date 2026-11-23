package com.example.inventoryordersystem.dto.response;

import com.example.inventoryordersystem.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderResponse {
    private Long id;
    private String orderCode;
    private Long customerId;
    private Long userId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime orderedAt;
    private List<OrderItemResponse> orderItems;

    @Getter
    @Builder
    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}


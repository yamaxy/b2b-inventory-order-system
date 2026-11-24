package com.example.inventoryordersystem.dto.response;

import com.example.inventoryordersystem.entity.Order;
import com.example.inventoryordersystem.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

    public static OrderResponse fromEntity(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(i -> OrderItemResponse.builder()
                        .id(i.getId())
                        .productId(i.getProduct().getId())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .customerId(order.getCustomer().getId())
                .userId(order.getUser().getId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .orderedAt(order.getOrderedAt())
                .orderItems(itemResponses)
                .build();
    }

    @Getter
    @Builder
    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}
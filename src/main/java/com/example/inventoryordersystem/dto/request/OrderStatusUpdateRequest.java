package com.example.inventoryordersystem.dto.request;

import com.example.inventoryordersystem.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateRequest {

    @NotNull(message = "遷移先のステータスを指定してください。")
    private OrderStatus status;
}

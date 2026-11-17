package com.example.inventoryordersystem.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class OrderCreateRequest {

    @NotNull(message = "取引先IDは必須です")
    private Long customerId;

    @NotEmpty(message = "受注明細は1件以上指定してください")
    @Valid
    private List<OrderItemRequest> orderItems;
}

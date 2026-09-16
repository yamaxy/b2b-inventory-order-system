package com.example.inventoryordersystem.dto.response;

import com.example.inventoryordersystem.entity.Customer;
import java.time.LocalDateTime;

public record CustomerResponse(
        Long id,
        String customerCode,
        String name,
        String phoneNumber,
        String address,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CustomerResponse fromEntity(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getCustomerCode(),
                customer.getName(),
                customer.getPhoneNumber(),
                customer.getAddress(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}

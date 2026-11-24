package com.example.inventoryordersystem.service;

import com.example.inventoryordersystem.dto.response.OrderResponse;
import com.example.inventoryordersystem.entity.*;
import com.example.inventoryordersystem.exception.BusinessException;
import com.example.inventoryordersystem.exception.ResourceNotFoundException;
import com.example.inventoryordersystem.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceStatusTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private Product product;
    private Order order;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .productCode("PRD-001")
                .name("テスト商品")
                .price(BigDecimal.valueOf(1000))
                .currentStock(20)
                .safetyStock(5)
                .build();

        Customer customer = Customer.builder()
                .id(1L)
                .name("テスト顧客")
                .build();

        User user = User.builder()
                .id(1L)
                .email("staff01@example.com")
                .passwordHash("encodedPassword")
                .name("担当者01")
                .role(Role.STAFF)
                .build();

        OrderItem item = OrderItem.builder()
                .id(1L)
                .product(product)
                .quantity(5)
                .unitPrice(BigDecimal.valueOf(1000))
                .build();

        order = Order.builder()
                .id(100L)
                .orderCode("ORD-20261124-001")
                .customer(customer)
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(5000))
                .orderedAt(LocalDateTime.now())
                .orderItems(List.of(item))
                .build();
    }

    @Test
    @DisplayName("PENDINGからPREPARINGへ遷移でき、在庫数は変動しないこと")
    void updateStatus_pendingToPreparing_success() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.updateOrderStatus(100L, OrderStatus.PREPARING);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.PREPARING);
        assertThat(product.getCurrentStock()).isEqualTo(20);
    }

    @Test
    @DisplayName("PREPARINGからCOMPLETEDへ遷移すると、実在庫数が数量分減算されること")
    void updateStatus_preparingToCompleted_decreasesStock() {
        order.setStatus(OrderStatus.PREPARING);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.updateOrderStatus(100L, OrderStatus.COMPLETED);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(product.getCurrentStock()).isEqualTo(15);
    }

    @Test
    @DisplayName("PREPARINGからCANCELLEDへ遷移すると、在庫数が数量分還元されること")
    void updateStatus_preparingToCancelled_restoresStock() {
        order.setStatus(OrderStatus.PREPARING);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.updateOrderStatus(100L, OrderStatus.CANCELLED);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product.getCurrentStock()).isEqualTo(25);
    }

    @Test
    @DisplayName("存在しない受注IDを指定した場合はResourceNotFoundExceptionが発生すること")
    void updateStatus_orderNotFound_throwsException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus(999L, OrderStatus.PREPARING))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("COMPLETED遷移時に在庫が不足している場合はBusinessExceptionが発生すること")
    void updateStatus_completedWhenStockInsufficient_throwsException() {
        order.setStatus(OrderStatus.PREPARING);
        product.setCurrentStock(3);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(100L, OrderStatus.COMPLETED))
                .isInstanceOf(BusinessException.class);
        assertThat(product.getCurrentStock()).isEqualTo(3);
    }
}

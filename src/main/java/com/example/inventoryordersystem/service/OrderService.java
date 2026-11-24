package com.example.inventoryordersystem.service;

import com.example.inventoryordersystem.dto.request.OrderCreateRequest;
import com.example.inventoryordersystem.dto.request.OrderItemRequest;
import com.example.inventoryordersystem.dto.response.OrderResponse;
import com.example.inventoryordersystem.entity.*;
import com.example.inventoryordersystem.exception.BusinessException;
import com.example.inventoryordersystem.exception.ResourceNotFoundException;
import com.example.inventoryordersystem.repository.CustomerRepository;
import com.example.inventoryordersystem.repository.OrderRepository;
import com.example.inventoryordersystem.repository.ProductRepository;
import com.example.inventoryordersystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private record PreparedItem(Product product, Integer quantity, BigDecimal unitPrice) {
    }

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request, String currentUsername) {
        // 1. 取引先の存在確認
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("指定された取引先が存在しません ID: " + request.getCustomerId()));

        // 2. 注文者（ユーザー）の取得
        User user = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("ログインユーザーが見つかりません: " + currentUsername));

        // 3. 受注コードの自動生成
        String orderCode = "ORD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PreparedItem> preparedItems = new ArrayList<>();

        // 4. 明細ごとの在庫チェックと現在庫の減算処理（findById は1回のみ）
        for (OrderItemRequest itemReq : request.getOrderItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("指定された商品が存在しません ID: " + itemReq.getProductId()));

            // 現在庫数のチェック
            if (product.getCurrentStock() < itemReq.getQuantity()) {
                throw new BusinessException("商品名: " + product.getName() + " の在庫が不足しています。（現在庫: " + product.getCurrentStock() + "）");
            }

            // 在庫の減算（@Versionによる楽観的ロックが有効に機能する）
            product.setCurrentStock(product.getCurrentStock() - itemReq.getQuantity());

            BigDecimal unitPrice = product.getPrice();
            BigDecimal subTotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(subTotal);

            preparedItems.add(new PreparedItem(product, itemReq.getQuantity(), unitPrice));
        }

        // 5. 確定した合計金額を含めて Order を構築し、明細を紐付け
        Order order = Order.builder()
                .orderCode(orderCode)
                .customer(customer)
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .orderedAt(LocalDateTime.now())
                .orderItems(new ArrayList<>())
                .build();

        for (PreparedItem item : preparedItems) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(item.product())
                    .quantity(item.quantity())
                    .unitPrice(item.unitPrice())
                    .build();

            order.getOrderItems().add(orderItem);
        }

        Order savedOrder = orderRepository.save(order);

        // 6. レスポンスDTOへの変換返却
        List<OrderResponse.OrderItemResponse> itemResponses = savedOrder.getOrderItems().stream()
                .map(i -> OrderResponse.OrderItemResponse.builder()
                        .id(i.getId())
                        .productId(i.getProduct().getId())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(savedOrder.getId())
                .orderCode(savedOrder.getOrderCode())
                .customerId(customer.getId())
                .userId(user.getId())
                .status(savedOrder.getStatus())
                .totalAmount(savedOrder.getTotalAmount())
                .orderedAt(savedOrder.getOrderedAt())
                .orderItems(itemResponses)
                .build();
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus nextStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("受注が見つかりません。ID: " + orderId));

        OrderStatus currentStatus = order.getStatus();
        currentStatus.validateTransitionTo(nextStatus);

        if (nextStatus == OrderStatus.COMPLETED) {
            // 出荷済（COMPLETED）遷移時：現在庫数の減算
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                int quantity = item.getQuantity();

                int updatedStock = product.getCurrentStock() - quantity;
                if (updatedStock < 0) {
                    throw new BusinessException(
                            "商品名: " + product.getName() + " の在庫が不足しています。（現在庫: " + product.getCurrentStock() + "）"
                    );
                }

                product.setCurrentStock(updatedStock);
            }
        } else if (nextStatus == OrderStatus.CANCELLED) {
            // キャンセル（CANCELLED）遷移時：在庫数の還元
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                int quantity = item.getQuantity();

                product.setCurrentStock(product.getCurrentStock() + quantity);
            }
        }

        order.setStatus(nextStatus);
        Order savedOrder = orderRepository.save(order);
        return OrderResponse.fromEntity(savedOrder);
    }
}
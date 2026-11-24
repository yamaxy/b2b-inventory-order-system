package com.example.inventoryordersystem.service;

import com.example.inventoryordersystem.entity.Customer;
import com.example.inventoryordersystem.entity.Order;
import com.example.inventoryordersystem.entity.OrderItem;
import com.example.inventoryordersystem.entity.OrderStatus;
import com.example.inventoryordersystem.entity.Product;
import com.example.inventoryordersystem.entity.Role;
import com.example.inventoryordersystem.entity.User;
import com.example.inventoryordersystem.repository.CustomerRepository;
import com.example.inventoryordersystem.repository.OrderRepository;
import com.example.inventoryordersystem.repository.ProductRepository;
import com.example.inventoryordersystem.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OrderServiceStatusExceptionTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @ParameterizedTest
    @DisplayName("異常系：許可されていないステータス遷移の場合は例外が発生すること")
    @CsvSource({
            "PENDING, COMPLETED",
            "PREPARING, PENDING",
            "COMPLETED, PREPARING",
            "COMPLETED, CANCELLED",
            "CANCELLED, PREPARING",
            "CANCELLED, COMPLETED"
    })
    @Transactional
    void updateStatus_invalidTransition_throwsException(OrderStatus currentStatus, OrderStatus nextStatus) {
        Customer customer = customerRepository.save(Customer.builder()
                .customerCode("CUST-INV-" + currentStatus.name() + "-" + nextStatus.name())
                .name("遷移テスト取引先")
                .phoneNumber("03-1111-2222")
                .address("東京都千代田区")
                .build());

        User user = userRepository.save(User.builder()
                .email("staff-inv-" + currentStatus.name() + "-" + nextStatus.name() + "@example.com")
                .passwordHash("encodedPassword")
                .name("担当者")
                .role(Role.STAFF)
                .build());

        Product product = productRepository.save(Product.builder()
                .productCode("PROD-INV-" + currentStatus.name() + "-" + nextStatus.name())
                .name("遷移テスト商品")
                .price(BigDecimal.valueOf(1000))
                .currentStock(20)
                .safetyStock(5)
                .build());

        Order order = Order.builder()
                .orderCode("ORD-INV-" + currentStatus.name() + "-" + nextStatus.name())
                .customer(customer)
                .user(user)
                .status(currentStatus)
                .totalAmount(BigDecimal.valueOf(2000))
                .orderedAt(LocalDateTime.now())
                .orderItems(new ArrayList<>())
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(1000))
                .build();

        order.getOrderItems().add(item);

        Order savedOrder = orderRepository.save(order);

        assertThatThrownBy(() -> orderService.updateOrderStatus(savedOrder.getId(), nextStatus))
                .isInstanceOf(IllegalStateException.class);
        assertThat(product.getCurrentStock()).isEqualTo(20);
    }

    @Test
    @DisplayName("異常系：ステータス更新時に楽観的ロック競合が発生した場合はObjectOptimisticLockingFailureExceptionとなること")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void updateStatus_optimisticLockConflict_throwsException() {
        TransactionTemplate requiresNewTx = new TransactionTemplate(transactionManager);
        requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        Long targetProductId = null;
        Long targetCustomerId = null;
        Long targetUserId = null;

        try {
            record TestIds(Long productId, Long customerId, Long userId) {}

            TestIds ids = requiresNewTx.execute(status -> {
                Customer c = customerRepository.save(Customer.builder()
                        .customerCode("CUST-OPT-ST-001")
                        .name("競合テスト取引先")
                        .phoneNumber("03-9999-0000")
                        .address("東京都品川区")
                        .build());
                User u = userRepository.save(User.builder()
                        .email("staff-opt-st-001@example.com")
                        .passwordHash("encodedPassword")
                        .name("競合テスト担当者")
                        .role(Role.STAFF)
                        .build());
                Product p = productRepository.save(Product.builder()
                        .productCode("PROD-OPT-ST-001")
                        .name("競合テスト商品")
                        .price(BigDecimal.valueOf(1000))
                        .currentStock(20)
                        .safetyStock(5)
                        .build());
                return new TestIds(p.getId(), c.getId(), u.getId());
            });

            targetProductId = ids.productId();
            targetCustomerId = ids.customerId();
            targetUserId = ids.userId();

            // 古い version を保持したエンティティを取得
            Product staleProduct = productRepository.findById(targetProductId).orElseThrow();

            // 別トランザクションで先に更新・コミットして DB 側の version を進める
            final Long productIdForUpdate = targetProductId;
            requiresNewTx.executeWithoutResult(status -> {
                Product latestProduct = productRepository.findById(productIdForUpdate).orElseThrow();
                latestProduct.setCurrentStock(latestProduct.getCurrentStock() - 1);
                productRepository.saveAndFlush(latestProduct);
            });

            // 古い version を持つ staleProduct を別トランザクションで更新・フラッシュすると競合が発生
            staleProduct.setCurrentStock(staleProduct.getCurrentStock() - 5);

            assertThatThrownBy(() -> requiresNewTx.executeWithoutResult(
                    status -> productRepository.saveAndFlush(staleProduct)
            )).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        } finally {
            final Long cleanupProductId = targetProductId;
            final Long cleanupCustomerId = targetCustomerId;
            final Long cleanupUserId = targetUserId;

            if (cleanupProductId != null) {
                requiresNewTx.executeWithoutResult(status ->
                        productRepository.findById(cleanupProductId).ifPresent(productRepository::delete)
                );
            }
            if (cleanupCustomerId != null) {
                requiresNewTx.executeWithoutResult(status ->
                        customerRepository.findById(cleanupCustomerId).ifPresent(customerRepository::delete)
                );
            }
            if (cleanupUserId != null) {
                requiresNewTx.executeWithoutResult(status ->
                        userRepository.findById(cleanupUserId).ifPresent(userRepository::delete)
                );
            }
        }
    }
}

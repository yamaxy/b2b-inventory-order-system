package com.example.inventoryordersystem.controller;

import com.example.inventoryordersystem.dto.request.OrderCreateRequest;
import com.example.inventoryordersystem.dto.request.OrderItemRequest;
import com.example.inventoryordersystem.entity.Customer;
import com.example.inventoryordersystem.entity.Product;
import com.example.inventoryordersystem.entity.Role;
import com.example.inventoryordersystem.entity.User;
import com.example.inventoryordersystem.repository.CustomerRepository;
import com.example.inventoryordersystem.repository.ProductRepository;
import com.example.inventoryordersystem.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class OrderControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long savedCustomerId;
    private Long savedProductId;
    private static final String TEST_USER_EMAIL = "order.test.staff@example.com";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        objectMapper = new ObjectMapper();

        // 1. テスト用ユーザー（注文者）の準備（passwordHash を使用）
        userRepository.findByEmail(TEST_USER_EMAIL).orElseGet(() ->
                userRepository.save(User.builder()
                        .name("受注テスト担当者")
                        .email(TEST_USER_EMAIL)
                        .passwordHash(passwordEncoder.encode("Password123!"))
                        .role(Role.STAFF)
                        .build())
        );

        // 2. テスト用取引先の準備（既存があれば再利用し、なければ作成）
        Customer customer = customerRepository.findByCustomerCode("CUST-ORD-001")
                .orElseGet(() -> customerRepository.save(Customer.builder()
                        .customerCode("CUST-ORD-001")
                        .name("受注テスト株式会社")
                        .phoneNumber("03-1111-2222")
                        .address("東京都千代田区")
                        .build()));
        savedCustomerId = customer.getId();

        // 3. テスト用商品の準備（既存があれば在庫・価格をリセット、なければ作成）
        Product product = productRepository.findByProductCode("PROD-ORD-001")
                .map(existing -> {
                    existing.setName("受注テスト商品");
                    existing.setPrice(BigDecimal.valueOf(1500));
                    existing.setCurrentStock(20);
                    existing.setSafetyStock(5);
                    return productRepository.save(existing);
                })
                .orElseGet(() -> productRepository.save(Product.builder()
                        .productCode("PROD-ORD-001")
                        .name("受注テスト商品")
                        .price(BigDecimal.valueOf(1500))
                        .currentStock(20)
                        .safetyStock(5)
                        .build()));
        savedProductId = product.getId();
    }

    @Test
    @DisplayName("正常系：STAFF権限で新規受注が作成されること")
    @WithMockUser(username = TEST_USER_EMAIL, roles = "STAFF")
    void createOrder_success() throws Exception {
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(savedProductId);
        itemRequest.setQuantity(3);

        OrderCreateRequest createRequest = new OrderCreateRequest();
        createRequest.setCustomerId(savedCustomerId);
        createRequest.setOrderItems(List.of(itemRequest));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(savedCustomerId))
                .andExpect(jsonPath("$.totalAmount").value(4500));
    }

    @Test
    @DisplayName("異常系：在庫不足時に400系エラーとなること")
    @WithMockUser(username = TEST_USER_EMAIL, roles = "STAFF")
    void createOrder_insufficientStock() throws Exception {
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(savedProductId);
        itemRequest.setQuantity(999);

        OrderCreateRequest createRequest = new OrderCreateRequest();
        createRequest.setCustomerId(savedCustomerId);
        createRequest.setOrderItems(List.of(itemRequest));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("権限エラー：READONLY権限では403 Forbiddenとなること")
    @WithMockUser(username = "readonly@example.com", roles = "READONLY")
    void createOrder_forbiddenForReadonly() throws Exception {
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(savedProductId);
        itemRequest.setQuantity(1);

        OrderCreateRequest createRequest = new OrderCreateRequest();
        createRequest.setCustomerId(savedCustomerId);
        createRequest.setOrderItems(List.of(itemRequest));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("異常系：楽観的ロック競合（バージョン不整合）時にObjectOptimisticLockingFailureExceptionが発生すること")
    @WithMockUser(username = TEST_USER_EMAIL, roles = "STAFF")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void createOrder_optimisticLockConflict() {
        TransactionTemplate requiresNewTx = new TransactionTemplate(transactionManager);
        requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        Long targetProductId = null;
        Long targetCustomerId = null;

        try {
            // 1. 別トランザクションで商品を新規作成・コミット（DB上の version = 0 になる）
            Product createdProduct = requiresNewTx.execute(status -> {
                Customer c = customerRepository.save(Customer.builder()
                        .customerCode("CUST-OPT-001")
                        .name("競合テスト取引先")
                        .phoneNumber("03-9999-0000")
                        .address("東京都品川区")
                        .build());
                Product p = productRepository.save(Product.builder()
                        .productCode("PROD-OPT-001")
                        .name("競合テスト商品")
                        .price(BigDecimal.valueOf(1000))
                        .currentStock(20)
                        .safetyStock(5)
                        .build());
                return p;
            });
            targetProductId = createdProduct.getId();

            // 2. 古い version（0）を保持したエンティティを取得
            Product staleProduct = productRepository.findById(targetProductId).orElseThrow();

            // 3. 別トランザクション（REQUIRES_NEW）で先に更新・コミット（DB上の version が 1 に進む）
            final Long productIdForUpdate = targetProductId;
            requiresNewTx.executeWithoutResult(status -> {
                Product latestProduct = productRepository.findById(productIdForUpdate).orElseThrow();
                latestProduct.setCurrentStock(latestProduct.getCurrentStock() - 1);
                productRepository.saveAndFlush(latestProduct);
            });

            // 4. 古い version（0）を持つ staleProduct を別トランザクションで保存・フラッシュすると競合が発生
            staleProduct.setCurrentStock(staleProduct.getCurrentStock() - 1);

            org.junit.jupiter.api.Assertions.assertThrows(
                    org.springframework.orm.ObjectOptimisticLockingFailureException.class,
                    () -> requiresNewTx.executeWithoutResult(status -> productRepository.saveAndFlush(staleProduct))
            );
        } finally {
            // @Transactional(NOT_SUPPORTED) でコミットされたテストデータを後片付け
            final Long cleanupProductId = targetProductId;
            if (cleanupProductId != null) {
                requiresNewTx.executeWithoutResult(status ->
                        productRepository.findById(cleanupProductId).ifPresent(productRepository::delete)
                );
            }
            customerRepository.findAll().stream()
                    .filter(c -> "CUST-OPT-001".equals(c.getCustomerCode()))
                    .findFirst()
                    .ifPresent(c -> requiresNewTx.executeWithoutResult(status -> customerRepository.delete(c)));
        }
    }

}
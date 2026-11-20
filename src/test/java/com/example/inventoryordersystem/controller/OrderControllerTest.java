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

        // 2. テスト用取引先の登録
        Customer customer = customerRepository.save(Customer.builder()
                .customerCode("CUST-ORD-001")
                .name("受注テスト株式会社")
                .phoneNumber("03-1111-2222")
                .address("東京都千代田区")
                .build());
        savedCustomerId = customer.getId();

        // 3. テスト用商品の登録（単価: 1500, 現在庫: 20, 安全在庫: 5）
        Product product = productRepository.save(Product.builder()
                .productCode("PROD-ORD-001")
                .name("受注テスト商品")
                .price(BigDecimal.valueOf(1500))
                .currentStock(20)
                .safetyStock(5)
                .build());
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
}
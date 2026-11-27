package com.example.inventoryordersystem.controller;

import com.example.inventoryordersystem.dto.request.InventoryAdjustmentRequest;
import com.example.inventoryordersystem.entity.Product;
import com.example.inventoryordersystem.entity.Role;
import com.example.inventoryordersystem.entity.StockChangeType;
import com.example.inventoryordersystem.entity.User;
import com.example.inventoryordersystem.repository.ProductRepository;
import com.example.inventoryordersystem.repository.StockHistoryRepository;
import com.example.inventoryordersystem.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class InventoryControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StockHistoryRepository stockHistoryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    private Product alertProduct;
    private User staffUser;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        alertProduct = productRepository.save(Product.builder()
                .productCode("INV-ALERT-" + suffix)
                .name("安全在庫割れテスト商品")
                .price(new BigDecimal("1500.00"))
                .currentStock(2)
                .safetyStock(10)
                .version(0)
                .build());

        staffUser = userRepository.findByEmail("staff-inv-test@example.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("staff-inv-test@example.com")
                        .passwordHash("encoded-password")
                        .name("在庫テスト担当者")
                        .role(Role.STAFF)
                        .build()));
    }

    @Test
    @DisplayName("リアルタイム在庫一覧取得：認証済みユーザーで 200 OK が返ること")
    @WithMockUser(username = "staff-inv-test@example.com", roles = "STAFF")
    void getInventoryList_Success() throws Exception {
        mockMvc.perform(get("/api/v1/inventory")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].productCode", hasItem(alertProduct.getProductCode())));
    }

    @Test
    @DisplayName("安全在庫アラート取得：安全在庫を下回る商品が返却されること")
    @WithMockUser(username = "staff-inv-test@example.com", roles = "STAFF")
    void getInventoryAlerts_Success() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].productCode", hasItem(alertProduct.getProductCode())))
                .andExpect(jsonPath("$[?(@.productCode == '" + alertProduct.getProductCode() + "')].shortageQuantity")
                        .value(hasItem(8)));
    }

    @Test
    @DisplayName("手動在庫調整：STAFF 権限で在庫更新と履歴作成が行われ 201 Created が返ること")
    @WithMockUser(username = "staff-inv-test@example.com", roles = "STAFF")
    void adjustStock_Success() throws Exception {
        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest(
                alertProduct.getId(),
                StockChangeType.INBOUND,
                15,
                "緊急入荷"
        );

        mockMvc.perform(post("/api/v1/inventory/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(alertProduct.getId()))
                .andExpect(jsonPath("$.userId").value(staffUser.getId()))
                .andExpect(jsonPath("$.changeType").value("INBOUND"))
                .andExpect(jsonPath("$.quantityChange").value(15))
                .andExpect(jsonPath("$.currentStockAfter").value(17))
                .andExpect(jsonPath("$.reason").value("緊急入荷"));

        assertThat(stockHistoryRepository.count()).isGreaterThan(0);
    }

    @Test
    @DisplayName("手動在庫調整：READONLY 権限では 403 Forbidden が返ること")
    @WithMockUser(username = "readonly@example.com", roles = "READONLY")
    void adjustStock_Forbidden_AsReadonly() throws Exception {
        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest(
                alertProduct.getId(),
                StockChangeType.ADJUSTMENT,
                1,
                "権限不足テスト"
        );

        mockMvc.perform(post("/api/v1/inventory/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未認証アクセス：401 Unauthorized が返ること")
    void getInventory_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/inventory"))
                .andExpect(status().isUnauthorized());
    }
}
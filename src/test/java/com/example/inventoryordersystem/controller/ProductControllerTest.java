package com.example.inventoryordersystem.controller;

import com.example.inventoryordersystem.dto.request.ProductCreateRequest;
import com.example.inventoryordersystem.dto.request.ProductUpdateRequest;
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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional // テスト実行後にDBを自動ロールバックして状態を保護
class ProductControllerTest {

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    // -------------------------------------------------------------------
    // 1. 商品一覧・詳細取得 (GET /api/v1/products)
    // -------------------------------------------------------------------
    @Test
    @DisplayName("ログイン済みのユーザー（STAFF）は商品一覧を取得できる")
    @WithMockUser(roles = "STAFF")
    void getAllProducts_Success_AsStaff() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(0)));
    }

    @Test
    @DisplayName("未認証ユーザーが一覧取得を試みると401 Unauthorizedとなる")
    void getAllProducts_Unauthorized_WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("存在しないIDの商品詳細を取得しようとすると404 Not Foundとなる")
    @WithMockUser(roles = "STAFF")
    void getProductById_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/products/999999"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------
    // 2. 商品新規登録 (POST /api/v1/products)
    // -------------------------------------------------------------------
    @Test
    @DisplayName("ADMIN権限で正しく入力された場合、商品新規登録が成功し201 Createdが返る")
    @WithMockUser(roles = "ADMIN")
    void createProduct_Success() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setProductCode("TEST-PROD-999");
        request.setName("テスト用ゲーミングキーボード");
        request.setPrice(new BigDecimal("12800.00"));
        request.setCurrentStock(100);
        request.setSafetyStock(20);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.productCode").value("TEST-PROD-999"))
                .andExpect(jsonPath("$.name").value("テスト用ゲーミングキーボード"))
                .andExpect(jsonPath("$.price").value(12800.00))
                .andExpect(jsonPath("$.currentStock").value(100))
                .andExpect(jsonPath("$.safetyStock").value(20));
    }

    @Test
    @DisplayName("READONLY権限のユーザーが新規登録を試みると403 Forbiddenとなる")
    @WithMockUser(roles = "READONLY")
    void createProduct_Forbidden_AsReadonly() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setProductCode("TEST-PROD-888");
        request.setName("テスト商品");
        request.setPrice(new BigDecimal("1000.00"));
        request.setCurrentStock(10);
        request.setSafetyStock(2);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("不正な値（負の価格・負の在庫数・必須文字欠落）で登録しようとすると400 Bad Requestとなる")
    @WithMockUser(roles = "ADMIN")
    void createProduct_ValidationError() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setProductCode(""); // 不正（空文字）
        request.setName("");        // 不正（空文字）
        request.setPrice(new BigDecimal("-100.00")); // 不正（負の値）
        request.setCurrentStock(-1); // 不正（負の値）
        request.setSafetyStock(-5);  // 不正（負の値）

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details[0].field").exists());
    }

    @Test
    @DisplayName("既に存在する商品コードで新規登録を試みると409 Conflictとなる")
    @WithMockUser(roles = "ADMIN")
    void createProduct_Conflict_DuplicateProductCode() throws Exception {
        ProductCreateRequest request1 = new ProductCreateRequest();
        request1.setProductCode("DUP-PROD-001");
        request1.setName("初回登録商品");
        request1.setPrice(new BigDecimal("5000.00"));
        request1.setCurrentStock(10);
        request1.setSafetyStock(2);

        // 1回目の登録（成功）
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // 2回目の登録（同コードのため 409 Conflict）
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(request1)))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    // -------------------------------------------------------------------
    // 3. 商品ライフサイクルテスト（作成 -> 更新 -> 削除 -> 取得不可確認）
    // -------------------------------------------------------------------
    @Test
    @DisplayName("登録した商品を更新・削除する一連のシナリオテスト")
    @WithMockUser(roles = "STAFF")
    void productLifecycle_Create_Update_Delete() throws Exception {
        // Step 1: 新規作成 (STAFF権限)
        ProductCreateRequest createReq = new ProductCreateRequest();
        createReq.setProductCode("LIFECYCLE-PROD");
        createReq.setName("初期登録名");
        createReq.setPrice(new BigDecimal("3000.00"));
        createReq.setCurrentStock(50);
        createReq.setSafetyStock(10);

        String resString = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long createdId = objectMapper.readTree(resString).get("id").asLong();

        // Step 2: 更新 (PUT)
        ProductUpdateRequest updateReq = new ProductUpdateRequest();
        updateReq.setName("更新後商品名");
        updateReq.setPrice(new BigDecimal("3500.00"));
        updateReq.setCurrentStock(40);
        updateReq.setSafetyStock(15);

        mockMvc.perform(put("/api/v1/products/" + createdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("更新後商品名"))
                .andExpect(jsonPath("$.price").value(3500.00))
                .andExpect(jsonPath("$.currentStock").value(40))
                .andExpect(jsonPath("$.safetyStock").value(15));

        // Step 3: 削除 (DELETE)
        mockMvc.perform(delete("/api/v1/products/" + createdId))
                .andExpect(status().isNoContent());

        // Step 4: 削除後に詳細取得を試みて 404 となることを確認
        mockMvc.perform(get("/api/v1/products/" + createdId))
                .andExpect(status().isNotFound());
    }
}

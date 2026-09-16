package com.example.inventoryordersystem.controller;

import com.example.inventoryordersystem.dto.request.CustomerCreateRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional // テスト実行後にDBを自動ロールバックして状態を保護
class CustomerControllerTest {

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @DisplayName("取引先一覧取得：認証済みユーザーで取得できること")
    @WithMockUser(roles = "STAFF")
    void getCustomers_success() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("取引先新規作成：STAFF権限で正常作成できること")
    @WithMockUser(roles = "STAFF")
    void createCustomer_success() throws Exception {
        CustomerCreateRequest request = new CustomerCreateRequest(
                "CUST-TEST-01",
                "テスト取引先",
                "03-0000-0000",
                "東京都千代田区"
        );

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerCode").value("CUST-TEST-01"))
                .andExpect(jsonPath("$.name").value("テスト取引先"));
    }

    @Test
    @DisplayName("取引先削除：ADMIN権限で削除できること")
    @WithMockUser(roles = "ADMIN")
    void deleteCustomer_success() throws Exception {
        // 事前データ（Flywayで挿入されているデータなど）または新規追加データのIDを指定してテスト
        mockMvc.perform(delete("/api/v1/customers/1"))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("取引先削除：STAFF権限ではForbidden(403)エラーとなること")
    @WithMockUser(roles = "STAFF")
    void deleteCustomer_forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/1"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}

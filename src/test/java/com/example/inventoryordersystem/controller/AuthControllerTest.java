package com.example.inventoryordersystem.controller;

import com.example.inventoryordersystem.dto.request.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print; // ★ これを追加
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper(); // @Autowiredを外して直接注入

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Spring Security を適用した MockMvc インスタンスの手動構築
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @DisplayName("正しいユーザー名とパスワードでログインするとJWTトークンを取得できる")
    void loginSuccess() throws Exception {
        LoginRequest request = new LoginRequest("admin", "password");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print()) // ★ これを追加して詳細ログを出力
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("誤ったパスワードでログインすると例外が発生する")
    void loginFailure() throws Exception {
        LoginRequest request = new LoginRequest("admin", "wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized()); // 認証失敗時は通常 401 Unauthorized
    }

    @Test
    @DisplayName("ログインで取得したJWTトークンをヘッダーに付与すると保護されたエンドポイントにアクセスできる")
    void protectedEndpointAccessWithJwt() throws Exception {
        // 1. ログインしてトークンを取得
        LoginRequest loginRequest = new LoginRequest("admin", "password");
        String responseString = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(responseString).get("accessToken").asText();

        // 2. トークンなしでアクセス（401 未認証または 403 拒否）
        mockMvc.perform(get("/api/v1/health/protected"))
                .andExpect(status().isUnauthorized());

        // 3. トークンありでアクセス（200 OK）
        mockMvc.perform(get("/api/v1/health/protected")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
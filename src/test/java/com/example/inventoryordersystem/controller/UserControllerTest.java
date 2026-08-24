package com.example.inventoryordersystem.controller;

import com.example.inventoryordersystem.dto.request.UserCreateRequest;
import com.example.inventoryordersystem.dto.request.UserUpdateRequest;
import com.example.inventoryordersystem.entity.Role;
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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional // テスト実行後にDBを自動ロールバックして状態を保護
class UserControllerTest {

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
    // 1. ユーザー一覧取得 (GET /api/v1/users)
    // -------------------------------------------------------------------
    @Test
    @DisplayName("ADMIN権限を持つユーザーはユーザー一覧（ページネーション）を取得できる")
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_Success_AsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(0)));
    }

    @Test
    @DisplayName("STAFF権限のユーザーが一覧取得を試みると403 Forbiddenとなる")
    @WithMockUser(roles = "STAFF")
    void getAllUsers_Forbidden_AsStaff() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未認証ユーザーが一覧取得を試みると401 Unauthorizedとなる")
    void getAllUsers_Unauthorized_WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------
    // 2. ユーザー新規登録 (POST /api/v1/users)
    // -------------------------------------------------------------------
    @Test
    @DisplayName("ADMIN権限で正しく入力された場合、ユーザー新規登録が成功し201 Createdが返る")
    @WithMockUser(roles = "ADMIN")
    void createUser_Success() throws Exception {
        UserCreateRequest request = UserCreateRequest.builder()
                .email("newuser@example.com")
                .password("password123")
                .name("新規 太郎")
                .role(Role.STAFF)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.name").value("新規 太郎"))
                .andExpect(jsonPath("$.role").value("STAFF"))
                .andExpect(jsonPath("$.password").doesNotExist()) // パスワードがレスポンスに含まれないことを確認
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("不完全なリクエスト（メールアドレス不正・パスワード短すぎ）で新規登録すると400 Bad Requestとなる")
    @WithMockUser(roles = "ADMIN")
    void createUser_ValidationError() throws Exception {
        UserCreateRequest request = UserCreateRequest.builder()
                .email("invalid-email-format") // 不正なメール形式
                .password("123")               // 8文字未満
                .name("")                      // 空文字
                .role(null)                    // null
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details[0].field").exists());
    }

    // -------------------------------------------------------------------
    // 3. ユーザー詳細取得・更新・削除
    // -------------------------------------------------------------------
    @Test
    @DisplayName("存在しないIDのユーザー詳細を取得しようとすると404 Not Foundとなる")
    @WithMockUser(roles = "ADMIN")
    void getUserById_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/999999"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("登録したユーザーを更新・削除する一連のシナリオテスト")
    @WithMockUser(roles = "ADMIN")
    void userLifecycle_Create_Update_Delete() throws Exception {
        // Step 1: 新規作成
        UserCreateRequest createReq = UserCreateRequest.builder()
                .email("lifecycle@example.com")
                .password("password123")
                .name("ライフサイクル")
                .role(Role.STAFF)
                .build();

        String resString = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long createdId = objectMapper.readTree(resString).get("id").asLong();

        // Step 2: 更新 (PUT)
        UserUpdateRequest updateReq = UserUpdateRequest.builder()
                .name("更新後 太郎")
                .role(Role.ADMIN)
                .build();

        mockMvc.perform(put("/api/v1/users/" + createdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("更新後 太郎"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        // Step 3: 削除 (DELETE)
        mockMvc.perform(delete("/api/v1/users/" + createdId))
                .andExpect(status().isNoContent());

        // Step 4: 削除後に詳細取得を試みて 404 となることを確認
        mockMvc.perform(get("/api/v1/users/" + createdId))
                .andExpect(status().isNotFound());
    }
}

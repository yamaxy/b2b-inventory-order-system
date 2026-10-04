package com.example.inventoryordersystem.service;

import com.example.inventoryordersystem.config.JwtTokenProvider;
import com.example.inventoryordersystem.dto.request.LoginRequest;
import com.example.inventoryordersystem.dto.response.LoginResponse;
import com.example.inventoryordersystem.exception.AuthenticationFailedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("ログイン成功: 正しい認証情報でJWTトークンが返却されること")
    void login_Success() {
        // given
        LoginRequest request = new LoginRequest("admin", "password");
        given(tokenProvider.generateToken("admin", "ROLE_ADMIN")).willReturn("mocked-jwt-token");

        // when
        LoginResponse response = authService.login(request);

        // then
        assertThat(response.accessToken()).isEqualTo("mocked-jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("ログイン失敗: 不正なパスワードでAuthenticationFailedExceptionが発生すること")
    void login_InvalidCredentials_ThrowsException() {
        // given
        LoginRequest request = new LoginRequest("admin", "wrong-password");

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("ユーザー名またはパスワードが正しくありません");
    }
}

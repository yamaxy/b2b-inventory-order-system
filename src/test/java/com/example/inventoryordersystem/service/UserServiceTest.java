package com.example.inventoryordersystem.service;

import com.example.inventoryordersystem.dto.request.UserCreateRequest;
import com.example.inventoryordersystem.dto.request.UserUpdateRequest;
import com.example.inventoryordersystem.dto.response.UserResponse;
import com.example.inventoryordersystem.entity.Role;
import com.example.inventoryordersystem.entity.User;
import com.example.inventoryordersystem.exception.EmailAlreadyExistsException;
import com.example.inventoryordersystem.exception.ResourceNotFoundException;
import com.example.inventoryordersystem.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("ユーザー新規作成: 正常系（passwordHash フィールドを使用）")
    void createUser_Success() {
        // given
        UserCreateRequest request = UserCreateRequest.builder()
                .email("test@example.com")
                .password("rawPassword123")
                .name("テストユーザー")
                .role(Role.STAFF)
                .build();

        given(userRepository.existsByEmail("test@example.com")).willReturn(false);
        given(passwordEncoder.encode("rawPassword123")).willReturn("encodedPassword123");

        User savedUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("encodedPassword123") // password -> passwordHash に修正
                .name("テストユーザー")
                .role(Role.STAFF)
                .build();

        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        UserResponse response = userService.createUser(request);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getRole()).isEqualTo(Role.STAFF);
        verify(passwordEncoder).encode("rawPassword123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("ユーザー新規作成: Email重複による例外発生")
    void createUser_DuplicateEmail_ThrowsException() {
        // given
        UserCreateRequest request = UserCreateRequest.builder()
                .email("test@example.com")
                .password("password123")
                .name("テストユーザー")
                .role(Role.STAFF)
                .build();

        given(userRepository.existsByEmail("test@example.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    @DisplayName("ユーザー詳細取得: 存在しないID指定時に ResourceNotFoundException が発生すること")
    void getUserById_NotFound_ThrowsException() {
        // given
        Long nonExistentId = 99L;
        given(userRepository.findById(nonExistentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUserById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("ユーザー情報更新: 正常系")
    void updateUser_Success() {
        // given
        Long userId = 1L;
        User existingUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("encodedPassword123") // password -> passwordHash に修正
                .name("旧名前")
                .role(Role.STAFF)
                .build();

        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .name("新名前")
                .role(Role.ADMIN)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(existingUser));

        // when
        UserResponse response = userService.updateUser(userId, updateRequest);

        // then
        assertThat(response.getName()).isEqualTo("新名前");
        assertThat(response.getRole()).isEqualTo(Role.ADMIN);
    }
}

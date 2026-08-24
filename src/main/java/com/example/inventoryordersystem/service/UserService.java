package com.example.inventoryordersystem.service;

import com.example.inventoryordersystem.dto.request.UserCreateRequest;
import com.example.inventoryordersystem.dto.request.UserUpdateRequest;
import com.example.inventoryordersystem.dto.response.UserResponse;
import com.example.inventoryordersystem.entity.User;
import com.example.inventoryordersystem.exception.EmailAlreadyExistsException;
import com.example.inventoryordersystem.exception.ResourceNotFoundException;
import com.example.inventoryordersystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 1. ユーザー一覧取得（ページネーション対応）
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserResponse::fromEntity);
    }

    // 2. ユーザー詳細取得
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ユーザーが見つかりません。ID: " + id));
        return UserResponse.fromEntity(user);
    }

    // 3. ユーザー新規登録
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("このメールアドレスは既に登録されています: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword())) // パスワードのハッシュ化
                .name(request.getName())
                .role(request.getRole())
                .build();

        User savedUser = userRepository.save(user);
        return UserResponse.fromEntity(savedUser);
    }

    // 4. ユーザー更新（名前・ロールの更新）
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ユーザーが見つかりません。ID: " + id));

        user.setName(request.getName());
        user.setRole(request.getRole());

        return UserResponse.fromEntity(user);
    }

    // 5. ユーザー削除（物理削除）
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("ユーザーが見つかりません。ID: " + id);
        }
        userRepository.deleteById(id);
    }
}

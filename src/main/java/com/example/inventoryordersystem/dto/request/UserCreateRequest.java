package com.example.inventoryordersystem.dto.request;

import com.example.inventoryordersystem.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 生成経路を Builder に一本化して堅牢化
@Builder
public class UserCreateRequest {

    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレス形式で入力してください")
    private String email;

    @NotBlank(message = "パスワードは必須です")
    @Size(min = 8, message = "パスワードは8文字以上で指定してください")
    private String password;

    @NotBlank(message = "名前は必須です")
    private String name;

    @NotNull(message = "ロールは必須です")
    private Role role;
}

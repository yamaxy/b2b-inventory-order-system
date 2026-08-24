package com.example.inventoryordersystem.dto.request;

import com.example.inventoryordersystem.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 生成経路を Builder に一本化して堅牢化
@Builder
public class UserUpdateRequest {

    @NotBlank(message = "名前は必須です")
    private String name;

    @NotNull(message = "ロールは必須です")
    private Role role;
}

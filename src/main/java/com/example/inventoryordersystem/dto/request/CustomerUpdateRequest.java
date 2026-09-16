package com.example.inventoryordersystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerUpdateRequest(
        @NotBlank(message = "取引先名は必須です")
        @Size(max = 200, message = "取引先名は200文字以内で入力してください")
        String name,

        @Size(max = 20, message = "電話番号は20文字以内で入力してください")
        String phoneNumber,

        @Size(max = 500, message = "住所は500文字以内で入力してください")
        String address
) {}

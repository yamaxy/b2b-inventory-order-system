package com.example.inventoryordersystem.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        LocalDateTime timestamp,
        List<FieldErrorDetail> details
) {
    // バリデーションエラーや複数要素の不備情報を保持する不変クラス
    public record FieldErrorDetail(
            String field,
            String message
    ) {}

    // 詳細情報（details）なしの簡易生成メソッド
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path, LocalDateTime.now(), null);
    }

    // 詳細情報（details）ありの生成メソッド
    public static ErrorResponse of(int status, String error, String message, String path, List<FieldErrorDetail> details) {
        return new ErrorResponse(status, error, message, path, LocalDateTime.now(), details);
    }
}


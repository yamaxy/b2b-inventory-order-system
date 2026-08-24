package com.example.inventoryordersystem.exception;

import com.example.inventoryordersystem.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@SuppressWarnings("unused") // 未使用警告を抑制 ※実際にはSpringがリフレクションで自動呼び出ししている
public class GlobalExceptionHandler {

    // リクエストのバリデーションエラー（400 BAD REQUEST）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ErrorResponse.FieldErrorDetail> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErrorResponse.FieldErrorDetail(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed for request body",
                request.getRequestURI(),
                details
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 認証失敗時（401 UNAUTHORIZED）
    @ExceptionHandler({AuthenticationFailedException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            Exception ex, HttpServletRequest request) {

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    // 認可失敗・権限不足（403 FORBIDDEN）
    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            Exception ex, HttpServletRequest request) {

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "Access is denied: " + ex.getMessage(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    // 該当データが存在しない場合（404 NOT FOUND）
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    // メールアドレスの重複登録時（409 CONFLICT）
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExistsException(
            EmailAlreadyExistsException ex, HttpServletRequest request) {

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    // その他の予期せぬシステム例外（500 INTERNAL SERVER ERROR）
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception ex, HttpServletRequest request) {

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected error occurred: " + ex.getMessage(), // パラメータ未使用警告を抑制
                request.getRequestURI()
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

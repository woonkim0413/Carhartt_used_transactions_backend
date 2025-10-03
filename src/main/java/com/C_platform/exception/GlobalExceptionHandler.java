package com.C_platform.exception;


import com.C_platform.Member_woonkim.exception.KakaoOauthException;
import com.C_platform.global.ApiResponse;
import com.C_platform.global.Detail;
import com.C_platform.global.MetaData;
import com.C_platform.global.error.CommonErrorCode;
import com.C_platform.global.error.ErrorBody;
import com.C_platform.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.stream.Collectors;


@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        ArrayList<Detail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new Detail(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toCollection(ArrayList::new));

        ErrorBody<ErrorCode> errorBody = new ErrorBody<>(CommonErrorCode.VALIDATION_FAILED, details);

        MetaData meta = MetaData.builder()
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(ApiResponse.fail(errorBody, meta));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public void handleNoHandlerFoundException(NoHandlerFoundException ex) {
        log.error("No handler found: {}", ex.getMessage());
        // Handle the exception (e.g., return a custom response)
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public void handleHttpServerErrorException(HttpServerErrorException ex) {
        log.error("HTTP server error: {}", ex.getMessage());
        // Handle the exception (e.g., return a custom response)
    }

    // (운강) 공통 에러 응답 생성 utils method
    private static ResponseEntity<ApiResponse<Object>> getApiResponseResponseEntity(ErrorCode ex) {
        ErrorBody<ErrorCode> errorBody = new ErrorBody<>(ex);
        MetaData meta = MetaData.builder()
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.badRequest().body(ApiResponse.fail(errorBody, meta));
    }

    @ExceptionHandler(InvalidImageExtensionException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidImageExtensionException(InvalidImageExtensionException ex) {
        log.error("Invalid image extension: {}", ex.getMessage());
        return getApiResponseResponseEntity(ex.getErrorCode());
    }

    @ExceptionHandler(CategoryException.class)
    public ResponseEntity<ApiResponse<Object>> handleCategoryException(CategoryException ex) {
        log.error("Category error: {}", ex.getMessage());
        return getApiResponseResponseEntity(ex.getErrorCode());
    }

    //주문 생성 관련
    @ExceptionHandler(CreateOrderException.class)
    public ResponseEntity<ApiResponse<Object>> handleCreateOrderException(CreateOrderException ex) {
        log.error("CreateOrder error: {}", ex.getMessage());
        return getApiResponseResponseEntity(ex.getErrorCode()); // ← 공통 포맷 + HTTP 200 고정
    }

    // KakaoOauth Exception 관련 처리 handler
    @ExceptionHandler(KakaoOauthException.class)
    public ResponseEntity<ApiResponse<Object>> handleKakaoOauthException(KakaoOauthException ex) {
        log.error("KakaoOauth error: {}", ex.getMessage());
        return getApiResponseResponseEntity(ex.getErrorCode()); // ← 공통 포맷 + HTTP 200 고정
    }

    //결제 관련
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiResponse<Object>> handlePaymentException(PaymentException ex) {
        log.error("Payment error: {}", ex.getMessage());
        return getApiResponseResponseEntity(ex.getErrorCode()); // ← 공통 포맷 + HTTP 200 고정
    }
}


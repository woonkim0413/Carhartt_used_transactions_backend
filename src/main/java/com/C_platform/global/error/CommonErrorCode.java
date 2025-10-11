package com.C_platform.global.error;

import lombok.Getter;

@Getter
public enum CommonErrorCode implements ErrorCode {
    VALIDATION_FAILED("V001", "입력 값 유효성 검사에 실패했습니다."),
    INVALID_PARAMETER("V002", "유효하지 않은 요청 파라미터 입니다.");

    private final String code;
    private final String message;

    CommonErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}

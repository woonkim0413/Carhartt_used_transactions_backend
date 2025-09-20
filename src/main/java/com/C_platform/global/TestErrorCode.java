package com.C_platform.global;

import org.springframework.http.HttpStatus;

public enum TestErrorCode {
    TEST_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");

    private final HttpStatus code;
    private final String message;

    TestErrorCode(HttpStatus code, String message) {
        this.code = code;
        this.message = message;
    }

    public HttpStatus getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

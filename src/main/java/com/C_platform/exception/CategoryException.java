package com.C_platform.exception;

import com.C_platform.global.error.ErrorCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class CategoryException extends RuntimeException {
    private final ErrorCode errorCode;

    public CategoryException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

}

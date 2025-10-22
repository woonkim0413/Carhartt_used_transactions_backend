package com.C_platform.exception;

import com.C_platform.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class WishException extends RuntimeException {
    private final ErrorCode errorCode;

    public WishException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}

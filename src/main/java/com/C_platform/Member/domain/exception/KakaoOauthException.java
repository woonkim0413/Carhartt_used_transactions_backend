package com.C_platform.Member.domain.exception;

import com.C_platform.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class KakaoOauthException extends RuntimeException {
    private final ErrorCode errorCode;

    public KakaoOauthException(ErrorCode code) {
        super(code.getMessage());
        this.errorCode = getErrorCode();
    }
}

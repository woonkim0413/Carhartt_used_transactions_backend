package com.C_platform.Member_woonkim.exception;

import com.C_platform.global.error.ErrorCode;
import lombok.Getter;

/**
 * Local 인증 관련 예외
 *
 * Local 로그인, 회원가입 등 Local 인증 시 발생하는 예외입니다.
 */
@Getter
public class LocalAuthException extends RuntimeException {
    private final ErrorCode errorCode;

    public LocalAuthException(ErrorCode code) {
        super(code.getMessage());
        this.errorCode = code;
    }
}

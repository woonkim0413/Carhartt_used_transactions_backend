package com.C_platform.Member_woonkim.exception;

import com.C_platform.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class OauthException extends RuntimeException {
    private final ErrorCode errorCode;

    public OauthException(ErrorCode code) {
        super(code.getMessage());
        this.errorCode = code;
    }
}

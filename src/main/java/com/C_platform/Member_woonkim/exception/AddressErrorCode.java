package com.C_platform.Member_woonkim.exception;

import com.C_platform.global.error.ErrorCode;
import lombok.Getter;

@Getter
public enum AddressErrorCode implements ErrorCode {

    C001("CO10", "error 발생 지점에서 메세지 생성");

    private final String code;
    private final String messageKey;

    AddressErrorCode(String code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }

    @Override
    public String getMessage() {
        return messageKey;
    }
}

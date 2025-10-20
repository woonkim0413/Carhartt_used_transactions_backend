package com.C_platform.Member_woonkim.exception;

import com.C_platform.global.error.ErrorCode;
import lombok.Getter;

@Getter
public enum AddressErrorCode implements ErrorCode {

    C001("CO10", "주소 5개 초과 가변 메세지");

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

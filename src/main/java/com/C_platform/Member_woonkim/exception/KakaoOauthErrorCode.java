package com.C_platform.Member_woonkim.exception;

import com.C_platform.global.error.ErrorCode;
import lombok.Getter;

@Getter
public enum KakaoOauthErrorCode implements ErrorCode {
    C001("CO10", "Authorization Code를 받는 것에 실패했습니다"),
    C002("CO10", "ridirect 시점에 발급한 state code와 callback에서 가져온 state code가 서로 다릅니다"),
    C003("CO10", "현재 로그인 상태가 아닙니다");

    private final String code;
    private final String messageKey;

    KakaoOauthErrorCode(String code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }

    @Override
    public String getMessage() {
        return messageKey;
    }
}

package com.C_platform.Member_woonkim.exception;

import com.C_platform.global.error.ErrorCode;
import lombok.Getter;

@Getter
public enum OauthErrorCode implements ErrorCode {
    // Oauth 공통
    C001("CO10", "Authorization Code를 받는 것에 실패했습니다"),
    C002("CO08", "InMemory stateCode와 query parameter의 stateCode가 서로 다릅니다"),
    C003("C001", "현재 로그인 상태가 아닙니다"),
    C004("C008", "InMemory stateCode와 query parameter의 stateCode가 서로 다릅니다"),
    C005("C010", "Access Token을 받아오는 과정에서 에러가 발생하였습니다"),
    C006("C010", "Oauth 사용자 정보 요청에 실패하였습니다"),
    C008("C010", "local 기반 로그인 시 email을 필수로 넣어주어야 합니다"),
    C009("C010", "해당 로그인을 처리할 수 있는 provider을 지원하지 않습니다"),
    C010("C010", "해당 로그인을 처리할 수 있는 userForm을 지원하지 않습니다"),
    C011("C001", "세션이 만료됐거나 security 내부 처리 에러가 발생했습니다"),

    // Oauth naver 특화
    C007("C010", "[naver] Oauth 사용자 정보가 양식과 맞지 않습니다");

    // Oauth kakao 특화
    private final String code;
    private final String messageKey;

    OauthErrorCode(String code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }

    @Override
    public String getMessage() {
        return messageKey;
    }
}

package com.C_platform.Member_woonkim.exception;

import com.C_platform.global.error.ErrorCode;
import lombok.Getter;

/**
 * Local 인증 관련 에러 코드
 */
@Getter
public enum LocalAuthErrorCode implements ErrorCode {
    // Local 인증 공통
    C001("C001", "이메일 또는 비밀번호가 올바르지 않습니다"),
    C002("C002", "가입되지 않은 이메일입니다"),
    C003("C003", "로그인 상태가 아닙니다"),
    C004("C004", "로그아웃 실패입니다"),

    // 회원가입
    M001("M001", "회원을 찾을 수 없습니다"),
    M002("M002", "유효하지 않은 이메일 형식입니다"),
    M003("M003", "비밀번호가 정책을 충족하지 않습니다"),
    M004("M004", "유효하지 않은 이름입니다");

    private final String code;
    private final String messageKey;

    LocalAuthErrorCode(String code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }

    @Override
    public String getMessage() {
        return messageKey;
    }
}

package com.C_platform.Member_woonkim.exception;

import com.C_platform.global.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 이메일 검증 관련 에러 코드
 */
@Getter
@AllArgsConstructor
public enum EmailErrorCode implements ErrorCode {
    E001("E001", "이메일로 코드 전송 실패했습니다"),           // 메일 서버 오류
    E002("E002", "인증 코드가 만료되었습니다"),               // Redis에 코드 없음
    E003("E003", "인증 코드가 일치하지 않습니다"),           // 입력 코드 != 저장 코드
    E004("E004", "유효하지 않은 이메일입니다"),              // 형식 오류 또는 이미 가입된 회원
    E005("E005", "이미 가입된 회원의 이메일입니다"),              // 형식 오류 또는 이미 가입된 회원
    E006("E006", "이메일 인증을 수행해 주세요");              // 형식 오류 또는 이미 가입된 회원

    private final String code;
    private final String message;

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

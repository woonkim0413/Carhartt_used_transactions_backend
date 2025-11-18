package com.C_platform.Member_woonkim.domain.value;

import java.security.SecureRandom;

/**
 * 이메일 인증 코드 값 객체
 * 6자리 난수 코드를 생성하고 검증합니다.
 */
public class VerificationCode {
    private final String code;

    private VerificationCode(String code) {
        this.code = code;
    }

    /**
     * 6자리 난수 코드 생성
     * @return VerificationCode 객체
     */
    public static VerificationCode generate() {
        SecureRandom random = new SecureRandom();
        int randomNumber = random.nextInt(1000000); // 0 ~ 999999
        String code = String.format("%06d", randomNumber);
        return new VerificationCode(code);
    }

    /**
     * 코드 값 반환
     */
    public String getValue() {
        return code;
    }

    /**
     * 6자리 숫자 형식 검증
     */
    public boolean isValid() {
        return code.matches("^\\d{6}$");
    }

    @Override
    public String toString() {
        return code;
    }
}

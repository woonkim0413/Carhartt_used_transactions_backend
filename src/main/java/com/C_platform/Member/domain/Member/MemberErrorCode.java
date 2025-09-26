package com.C_platform.Member.domain.Member;

import com.C_platform.global.error.ErrorCode;
import lombok.Getter;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

@Getter
public enum MemberErrorCode implements ErrorCode {

    // ---------------- 일단 다이렉트로 오류 메세지 넣음 ----------------

    //    // === 인증 관련 ===
    //    O001("O001", "member.unauthenticated"),     // 로그인되지 않음
    //    O002("O002", "member.login.invalid"),       // 잘못된 로그인 정보
    //    O003("O003", "member.login.required"),      // 인증 필요
    //
    //    // === 회원 정보 관련 ===
    //    O004("O004", "member.not.found"),           // 회원 정보 없음
    //    O005("O005", "member.no.permission"),       // 접근 권한 없음
    //
    //    // === 서버 내부 ===
    //    O010("O010", "member.internal.error");      // 내부 처리 오류

    // ------------------------------------------------------------

    //    === 인증 관련 ===
    O001("O001", "로그인이 필요합니다."),                 // 로그인되지 않음
    O002("O002", "아이디 또는 비밀번호가 잘못되었습니다."), // 잘못된 로그인 정보
    O003("O003", "인증이 필요한 요청입니다."),             // 인증 필요

    // === 회원 정보 관련 ===
    O004("O004", "회원 정보를 찾을 수 없습니다."),         // 회원 정보 없음
    O005("O005", "이 요청을 수행할 권한이 없습니다."),     // 접근 권한 없음

    // === 서버 내부 ===
    O010("O010", "내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요."); // 내부 처리 오류

    private final String code;
    private final String messageKey;

    private static MessageSource messageSource;

    MemberErrorCode(String code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }

    public static void setMessageSource(MessageSource source) {
        messageSource = source;
    }

    @Override
    public String getMessage() {
        if (messageSource == null) {
            return "MessageSource is not initialized";
        }
        return messageSource.getMessage(this.messageKey, null, LocaleContextHolder.getLocale());
    }
}

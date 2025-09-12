package com.C_platform.Member.domain;

import org.springframework.security.core.context.SecurityContextHolder;

// 현재 로그인 중인 member_id를 주문이나 상품 domain에서 얻을 수 있도록 만들었습니다
public class CurrentMember {
    public Long idOrThrow() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            throw new IllegalStateException("미인증");

        Object principal = auth.getPrincipal();
        if (principal instanceof CustomOAuth2User u)
            return u.getMemberId();

        // 커스텀 principal이 아닌 경우 예외
        throw new IllegalStateException("지원하지 않는 Principal: " + principal.getClass().getSimpleName());
    }
}

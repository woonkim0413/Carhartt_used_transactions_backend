package com.C_platform.Member_woonkim.domain.member_enum;

import com.C_platform.Member_woonkim.domain.member_interface.Provider;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OAuthProvider implements Provider {
    KAKAO("KAKAO"),
    NAVER("NAVAER");

    private final String providerName;
}

package com.C_platform.Member_woonkim.domain.enums;

import com.C_platform.Member_woonkim.domain.interfaces.Provider;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OAuthProvider implements Provider {
    KAKAO("KAKAO"),
    NAVER("NAVAER");

    private final String providerName;
}

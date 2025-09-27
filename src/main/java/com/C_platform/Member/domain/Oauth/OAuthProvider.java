package com.C_platform.Member.domain.Oauth;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;

import java.io.Serializable;

@Getter
@AllArgsConstructor
public enum OAuthProvider implements Provider {
    KAKAO("KAKAO"),
    NAVER("NAVAER");

    private final String providerName;
}

package com.C_platform.Member.domain.Oauth;

import lombok.*;

import java.io.Serializable;

@Getter
@ToString
@Builder // @NoArgsContructor랑은 같이 사용할 수 없음 (생성 시점에 필드를 빌드해야 하기에)
// kakao Resource server에서 받은 Json data를 매핑
public class OAuth2KakaoUserInfoDto implements OAuthUserInfoDto {

    private final String id;
    private final int statusCode;
    private final String name;
    private final String email;
    private final OAuthProvider provider;
    private final LoginType loginType;

    @Builder
    public OAuth2KakaoUserInfoDto(String id, int statusCode, String name,
                                  String email, OAuthProvider provider, LoginType loginType) {
        this.id = id;
        this.statusCode = statusCode;
        this.name = name;
        this.email = email;
        this.provider = provider;
        this.loginType = loginType;
    }
}

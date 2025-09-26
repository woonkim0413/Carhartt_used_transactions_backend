package com.C_platform.Member.domain.Oauth;

import lombok.*;

@Getter
@ToString
@Builder // @NoArgsContructor랑은 같이 사용할 수 없음 (생성 시점에 필드를 빌드해야 하기에)
// kakao Resource server에서 받은 Json data를 매핑
public class OAuth2KakaoUserInfoDto {

    private final String id;
    private final int statusCode;
    private final String name;
    private final String email;

    @Builder
    public OAuth2KakaoUserInfoDto(String id, int statusCode, String name, String email) {
        this.id = id;
        this.statusCode = statusCode;
        this.name = name;
        this.email = email;
    }
}

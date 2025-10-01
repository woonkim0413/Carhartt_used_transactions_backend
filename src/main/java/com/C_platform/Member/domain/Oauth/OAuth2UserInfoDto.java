package com.C_platform.Member.domain.Oauth;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
// builder는 @NoArgsContructor랑은 같이 사용할 수 없음 (생성 시점에 필드를 빌드해야 하기에)
@Builder
// kakao Resource server에서 받은 Json data를 객체에 저장하기 위한 Dto
public class OAuth2UserInfoDto {
    // Resource server에서 사용자를 식별할 때 사용
    private final String id;
    // HTTP 상태 코드
    private final int statusCode;
    private final String name;
    private final String email;
    private final OAuthProvider provider;
    private final LoginType loginType;
}

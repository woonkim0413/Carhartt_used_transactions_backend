package com.C_platform.Member.domain.Oauth;

import lombok.*;

@Getter
@ToString
@Builder // @NoArgsContructor랑은 같이 사용할 수 없음 (생성 시점에 필드를 빌드해야 하기에)
// kakao Resource server에서 받은 Json data를 매핑
public class OAuth2KakaoUserInfoDto {

    private String id;
    private int statusCode;
    private String name;
    private String ageRange;
    private String birthday;
    private String gender;

    @Builder
    public OAuth2KakaoUserInfoDto(String id, int statusCode, String name, String ageRange, String birthday, String gender) {
        this.id = id;
        this.statusCode = statusCode;
        this.name = name;
        this.ageRange = ageRange;
        this.birthday = birthday;
        this.gender = gender;
    }
}

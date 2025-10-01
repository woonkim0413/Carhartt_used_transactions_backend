package com.C_platform.Member_woonkim.infrastructure.parser_and_register;

import com.C_platform.Member_woonkim.domain.member_enum.LoginType;
import com.C_platform.Member_woonkim.domain.member_enum.OAuthProvider;
import com.C_platform.Member_woonkim.domain.member_interface.Provider;
import com.C_platform.Member_woonkim.infrastructure.dto.OAuth2UserInfoDto;
import org.springframework.stereotype.Component;

import java.util.Map;

// Kakao Developer에서 동의한 사용자 정보 접근 scope에 해당하는 사용자 정보들
@Component
public class OAuth2KakaoUserInfoParser implements OAuth2UserInfoParser {

    public OAuth2UserInfoDto parse(Map<String, Object> userInfo) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");

        return OAuth2UserInfoDto.builder()
                .id(String.valueOf(userInfo.get("id")))
                .name((String) kakaoAccount.get("name"))
                .email((String) kakaoAccount.get("email"))
                .provider(OAuthProvider.KAKAO)
                .loginType(LoginType.OAUTH)
                .statusCode(200)
                .build();
    }

    // ParserRegistry 내부에서 Map의 key를 생성하기 위해 사용
    public Provider getProvider() {
        return OAuthProvider.KAKAO;
    }
}

package com.C_platform.Member_woonkim.infrastructure.register_and_userInfoParsers;

import com.C_platform.Member_woonkim.domain.enums.LoginType;
import com.C_platform.Member_woonkim.domain.enums.OAuthProvider;
import com.C_platform.Member_woonkim.domain.interfaces.Provider;
import com.C_platform.Member_woonkim.infrastructure.dto.OAuth2UserInfoDto;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OAuth2NaverUserInfoParser implements OAuth2UserInfoParser{
    @Override
    public OAuth2UserInfoDto parse(Map<String, Object> payload) {
        // payload: 네이버 userinfo의 "response" 맵 (이미 바깥 resultcode/message 제거된 상태)
        String id    = asString(payload.get("id"));      // 항상 제공
        String name  = asString(payload.get("name"));    // 동의 안 했으면 null
        if (isBlank(name)) {
            name = asString(payload.get("nickname"));    // nickname 동의 시 보조
        }
        String email = asString(payload.get("email"));   // 동의 안 했으면 null

        return OAuth2UserInfoDto.builder()
                .id(id)
                .name(emptyToNull(name))
                .email(emptyToNull(email))
                .provider(OAuthProvider.NAVER)
                .loginType(LoginType.OAUTH)
                .statusCode(200)
                .build();
    }

    @Override
    public Provider getProvider() {
        return OAuthProvider.NAVER;
    }

    private static String asString(Object v) { return v == null ? null : String.valueOf(v); }
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String emptyToNull(String s) { return isBlank(s) ? null : s; }
}

package com.C_platform.Member_woonkim.infrastructure.adapter;

import com.C_platform.Member_woonkim.application.port.OauthClientPort;
import com.C_platform.Member_woonkim.domain.enums.OAuthProvider;
import com.C_platform.Member_woonkim.infrastructure.dto.OAuth2UserInfoDto;
import org.springframework.stereotype.Component;

@Component
public class KakaoOAuthClient implements OauthClientPort {
    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.KAKAO;
    }

    @Override
    public String getAccessToken(String code) {
        return null;
    }

    @Override
    public OAuth2UserInfoDto getUserInfo(String accessToken) {
        return null;
    }
}

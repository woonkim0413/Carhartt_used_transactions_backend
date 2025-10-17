package com.C_platform.Member_woonkim.application.port;

import com.C_platform.Member_woonkim.infrastructure.dto.OAuth2UserInfoDto;
import com.C_platform.Member_woonkim.domain.enums.OAuthProvider;

import java.util.Map;

public interface OauthClientPort {
    OAuthProvider getProvider();
    String getAccessToken(String stateCode, String oauthCode, OAuthProvider provider);
    Map<String, Object> getUserInfo(String accessToken, OAuthProvider provider);
}

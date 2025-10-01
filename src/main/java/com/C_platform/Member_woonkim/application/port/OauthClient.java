package com.C_platform.Member_woonkim.application.port;

import com.C_platform.Member_woonkim.infrastructure.dto.OAuth2UserInfoDto;
import com.C_platform.Member_woonkim.domain.member_enum.OAuthProvider;

public interface OauthClient {
    OAuthProvider getProvider();
    String getAccessToken(String code);
    OAuth2UserInfoDto getUserInfo(String accessToken);
}

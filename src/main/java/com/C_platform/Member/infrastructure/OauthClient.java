package com.C_platform.Member.infrastructure;

import com.C_platform.Member.domain.Oauth.OAuth2UserInfoDto;
import com.C_platform.Member.domain.Oauth.OAuthProvider;

public interface OauthClient {
    OAuthProvider getProvider();
    String getAccessToken(String code);
    OAuth2UserInfoDto getUserInfo(String accessToken);
}

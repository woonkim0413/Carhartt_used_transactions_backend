package com.C_platform.Member.infrastructure;

import com.C_platform.Member.domain.Oauth.OAuthProvider;
import com.C_platform.Member.domain.Oauth.OAuthUserInfoDto;

public interface OauthClient {
    OAuthProvider getProvider();
    String getAccessToken(String code);
    OAuthUserInfoDto getUserInfo(String accessToken);
}

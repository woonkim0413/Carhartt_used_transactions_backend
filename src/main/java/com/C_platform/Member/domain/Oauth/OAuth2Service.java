package com.C_platform.Member.domain.Oauth;

public interface OAuth2Service {
    OAuth2KakaoUserInfoDto getUserInfo(String accessToken, OAuthProvider provider);

    String getAuthorizeUrl(OAuthProvider provider);

    String getAccessToken(String code, OAuthProvider provider);
}

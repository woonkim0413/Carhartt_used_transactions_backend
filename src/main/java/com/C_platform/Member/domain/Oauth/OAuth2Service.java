package com.C_platform.Member.domain.Oauth;

import com.C_platform.Member.ui.dto.LoginProviderDto;

import java.util.List;

public interface OAuth2Service {
    OAuth2KakaoUserInfoDto getUserInfo(String accessToken, OAuthProvider provider);

    String getAuthorizeUrl(OAuthProvider provider, String state);

    String getAccessToken(String code, OAuthProvider provider);

    List<LoginProviderDto> getLoginProviderList();
}

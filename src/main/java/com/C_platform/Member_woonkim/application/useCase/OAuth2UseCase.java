package com.C_platform.Member_woonkim.application.useCase;

import com.C_platform.Member_woonkim.application.port.OauthClientPort;
import com.C_platform.Member_woonkim.domain.dto.JoinOrLoginResult;
import com.C_platform.Member_woonkim.domain.enums.OAuthProvider;
import com.C_platform.Member_woonkim.domain.service.OAuth2Service;
import com.C_platform.Member_woonkim.presentation.dto.Oauth.response.LoginProviderResponseDto;
import com.C_platform.annotation.UseCase;
import lombok.RequiredArgsConstructor;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class OAuth2UseCase {

    private final OAuth2Service oauth2Service;
    private final OauthClientPort oauthClientPort;

    // getLoginProviders에서 호출
    public List<LoginProviderResponseDto> loginProviderList() {
        return oauth2Service.getLoginProviderList();
    }

    // redirectToKakao에서 호출
    public String AuthorizeUrl(OAuthProvider provider, String stateCode) {
        return oauth2Service.getAuthorizeUrl(provider, stateCode);
    }

    // kakaoCallback에서 호출
    public JoinOrLoginResult getUserInfo(String accessToken, OAuthProvider provider) {

    }
}

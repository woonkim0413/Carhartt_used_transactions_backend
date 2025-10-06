package com.C_platform.Member_woonkim.application.useCase;

import com.C_platform.Member_woonkim.application.port.OauthClientPort;
import com.C_platform.Member_woonkim.domain.dto.JoinOrLoginResult;
import com.C_platform.Member_woonkim.domain.enums.OAuthProvider;
import com.C_platform.Member_woonkim.domain.service.MemberJoinService;
import com.C_platform.Member_woonkim.domain.service.OAuth2Service;
import com.C_platform.Member_woonkim.infrastructure.dto.OAuth2UserInfoDto;
import com.C_platform.Member_woonkim.infrastructure.parser_and_register.ParserRegistry;
import com.C_platform.Member_woonkim.presentation.dto.Oauth.response.LoginProviderResponseDto;
import com.C_platform.annotation.UseCase;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@UseCase
@RequiredArgsConstructor
public class OAuth2UseCase {

    private final OAuth2Service oauth2Service;

    // TODO : (나중에) Runtime에 OauthProvider에 의해 DI를 다르게 주입 받는 방식 찾기
    private final OauthClientPort oauthClientPort;

    private final ParserRegistry parserRegistry; /// Resource server Json을 Dto로 만들어줄 전용 Parser을 Provider에 따라 반환

    private final MemberJoinService memberJoinService; /// 기존 회원 유무에 따른 서비스 제공

    // getLoginProviders에서 호출
    public List<LoginProviderResponseDto> loginProviderList() {
        return oauth2Service.getLoginProviderList();
    }

    // redirectToKakao에서 호출
    public String AuthorizeUrl(OAuthProvider provider, String stateCode) {
        return oauth2Service.getAuthorizeUrl(provider, stateCode);
    }

    // kakaoCallback에서 호출
    public OAuth2UserInfoDto getUserInfo(String stateCode, OAuthProvider provider) {
        // 1. Authorization server에 접근하여 accessToken 획득
        String accessToken = oauthClientPort.getAccessToken(stateCode, provider);

        // 2. Resource server에 접근하여 사용자 정보 획득
        Map<String, Object> userInfo = oauthClientPort.getUserInfo(accessToken, provider);

        // 3. provider에 맞는 parser을 반환, parser는 userInfo를 Dto로 전환
        return parserRegistry.of(provider).parse(userInfo);
    }

    public OAuth2UserInfoDto getUserInfoForCustom(String accessToken, OAuthProvider provider) {
        // 1. Resource server에 접근하여 사용자 정보 획득
        Map<String, Object> userInfo = oauthClientPort.getUserInfo(accessToken, provider);

        // 2. provider에 맞는 parser을 반환, parser는 userInfo를 Dto로 전환
        return parserRegistry.of(provider).parse(userInfo);
    }

    public JoinOrLoginResult ensureOAuthMember(OAuth2UserInfoDto userInfo, OAuthProvider provider) {
        return memberJoinService.ensureOAuthMember (
                provider,
                userInfo.getId(),
                userInfo.getName(),
                userInfo.getEmail()
        );
    }

}

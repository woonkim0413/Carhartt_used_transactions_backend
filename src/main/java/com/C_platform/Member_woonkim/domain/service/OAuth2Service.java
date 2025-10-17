package com.C_platform.Member_woonkim.domain.service;


import com.C_platform.Member_woonkim.infrastructure.dto.OAuth2ProviderPropertiesDto;
import com.C_platform.Member_woonkim.infrastructure.dto.OAuth2RegistrationPropertiesDto;
import com.C_platform.Member_woonkim.domain.enums.LoginType;
import com.C_platform.Member_woonkim.domain.enums.OAuthProvider;
import com.C_platform.Member_woonkim.presentation.dto.Oauth.response.LoginProviderResponseDto;
import com.C_platform.Member_woonkim.utils.OauthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;

import static org.springframework.web.reactive.function.server.RequestPredicates.queryParam;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2Service {
    // application-oauth2-{환경 profils}.yml에서 값 가져옴
    @Value("${app.base-url}")
    private String baseUrl; // 예: https://api.your-domain.com/v1/

    // properties.oauth.yml을 읽어서 Dto로 반환 해주는 객체
    private final OauthProperties oauthProperties;

    public List<LoginProviderResponseDto> getLoginProviderList() {
        return List.of(
                LoginProviderResponseDto.builder()
                        .provider(OAuthProvider.KAKAO)
                        .loginType(LoginType.OAUTH)
                        .authorizeUrl(baseUrl + "oauth/login/kakao")
                        .build(),

                LoginProviderResponseDto.builder()
                        .provider(OAuthProvider.NAVER)
                        .loginType(LoginType.OAUTH)
                        .authorizeUrl(baseUrl + "oauth/login/naver")
                        .build()
        );
    }

    // Oauth 인증 페이지 redirect url 생성 method
    public String getAuthorizeUrl(OAuthProvider provider, String state) {
        // 설정 파일에 저장된 provider에 따른 oauth 관련 정보 가져옴
        OAuth2RegistrationPropertiesDto.RegistrationConfig registration = oauthProperties.getRegistrationConfig(provider); // clientId, redirectUri, scope(List<String>) 등
        OAuth2ProviderPropertiesDto.ProviderConfig providerConfig = oauthProperties.getProviderConfig(provider);   // authorizationUri 등

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(providerConfig.authorizationUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", registration.clientId())
                .queryParam("redirect_uri", registration.redirectUri())
                .queryParam("state", state);

        // 쿠키에 sessionId가 있을 때를 test하기 위해서 주석
        // .queryParam("prompt", "login");

        // 중복 인코딩 방지
        return builder.build(false).toUriString();
    }
}
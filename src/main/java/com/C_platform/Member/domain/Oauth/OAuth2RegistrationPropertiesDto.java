package com.C_platform.Member.domain.Oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// application.oauth2.yml에서 설정 파일을 읽어서 저장하기 위한 DTO임
@ConfigurationProperties(prefix = "spring.security.oauth2.client.registration")
public record OAuth2RegistrationPropertiesDto (
    RegistrationConfig kakao,
    RegistrationConfig naver
) {
    public record RegistrationConfig(
            String clientId,
            String clientSecret,
            String redirectUri,
            String authorizationGrantType,
            String clientAuthenticationMethod,
            String clientName,
            String account_email,
            List<String> scope
    ) {}
}

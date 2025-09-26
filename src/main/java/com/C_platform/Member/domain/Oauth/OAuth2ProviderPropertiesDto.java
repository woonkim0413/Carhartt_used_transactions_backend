package com.C_platform.Member.domain.Oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

// application.oauth2.yml에서 설정 파일을 읽어서 저장하기 위한 DTO
@ConfigurationProperties(prefix = "spring.security.oauth2.client.provider")
public record OAuth2ProviderPropertiesDto(ProviderConfig kakao, ProviderConfig naver) {

    public record ProviderConfig(
            String authorizationUri,
            String tokenUri,
            String userInfoUri
            // String userNameAttribute
    ) {
    }
}

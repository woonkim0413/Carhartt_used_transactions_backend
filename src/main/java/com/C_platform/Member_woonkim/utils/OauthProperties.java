package com.C_platform.Member_woonkim.utils;

import com.C_platform.Member_woonkim.domain.enums.OAuthProvider;
import com.C_platform.Member_woonkim.infrastructure.dto.OAuth2ProviderPropertiesDto;
import com.C_platform.Member_woonkim.infrastructure.dto.OAuth2RegistrationPropertiesDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OauthProperties {

    private final OAuth2RegistrationPropertiesDto registrationPropertiesDto;
    private final OAuth2ProviderPropertiesDto providerPropertiesDto;

    // properties.oauth.yml에서 등록 정보 (client id, client secrert, redirect url)를 가져옴
    public OAuth2RegistrationPropertiesDto.RegistrationConfig getRegistrationConfig(OAuthProvider provider) {
        return switch (provider) {
            case KAKAO -> registrationPropertiesDto.kakao();
            case NAVER -> registrationPropertiesDto.naver();
        };
    }

    // properties.oauth.yml에서 Oauth server와 통신하기 위한 url 정보들을 가져옴
    public OAuth2ProviderPropertiesDto.ProviderConfig getProviderConfig(OAuthProvider provider) {
        return switch (provider) {
            case KAKAO -> providerPropertiesDto.kakao();
            case NAVER -> providerPropertiesDto.naver();
        };
    }

}

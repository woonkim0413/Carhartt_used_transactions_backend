package com.C_platform.Member_woonkim.domain.service;

import com.C_platform.Member_woonkim.domain.enums.OAuthProvider;
import com.C_platform.Member_woonkim.infrastructure.dto.OAuth2UserInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collections;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final OAuth2Service oauth2Service;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        // 1. Kakao access_token 획득
        String accessToken = userRequest.getAccessToken().getTokenValue();

        // 2. 어떤 Provider인지 구분 (kakao, naver 등)
        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // "kakao"

        OAuthProvider provider = OAuthProvider.valueOf(registrationId.toUpperCase());

        // 3. 사용자 정보 조회
        OAuth2UserInfoDto User = oauth2Service.getUserInfo(accessToken, provider);

        // 4. 사용자 정보를 attributes에 넣음 (session 용)
        Map<String, Object> attributes = Map.of(
                "id", User.getId(),
                "name", User.getName(),
                "email", User.getEmail()
        );

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "id" // 고유 식별자 키
        );
    }
}

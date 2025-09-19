package com.C_platform.Member.domain.Oauth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2ServiceImpl implements OAuth2Service {

    // properties.oauth.yml을 읽어서 자바 코드에 대입하는 Dto들
    private final OAuth2RegistrationPropertiesDto registrationProps;
    private final OAuth2ProviderPropertiesDto providerProps;
    // Request API Utils
    private final RestTemplate restTemplate;
    // 해당 객체를 getUserInfo 내부로 들인 뒤 provider가 KAKAO인 경우만 new로 생성하는 패턴도 생각해봄
    // -> 안티 패턴임 new로 생성하면 spring이 bean으로 관리 못 하기에 AOP 지원 불가, TEST Mock 주입 불가함
    //    그리고 실질적인 Dto build는 .parse()를 호출할 때 실행되기에 resource 낭비도 거의 없다
    private final OAuth2KakaoService kakaoService;

    @Override
    // *** Access-Token으로 Resource Server에 사용자 정보를 요청하여 받은 뒤 dto에 담아서 반환하는 method ***
    public OAuth2KakaoUserInfoDto getUserInfo(String accessToken, OAuthProvider provider) {
        // provider에 따른 Oauth 등록 객체 반환
        OAuthRegistration registration = getRegistration(provider);

        // Resource Server에 보낼 요청 생성
        // Authorization Code를 Access-Token으로 교환할 때 client secret필요함 사용자 정보 단계에선 불필요
        RequestEntity<Void> request = RequestEntity
                .get(URI.create(registration.getUserInfoUri()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .build();

        // Resource Server에서 받아올 사용자 정보 응답
        ResponseEntity<Map> response;

        try {
            // restTemplate.exchainge()가 실행되는 시점에 요청이 보내짐
            response = restTemplate.exchange(request, Map.class);
        } catch (RestClientException e) {
            throw new RuntimeException("OAuth 사용자 정보 요청 실패", e);
        }

        // response에 실린 사용자 정보 획득
        Map<String, Object> userInfo = response.getBody();

        // 사용자 정보를 Dto에 담은 뒤 반환
        return switch (provider) {
            case KAKAO -> kakaoService.parse(userInfo);
            case NAVER -> throw new UnsupportedOperationException("Naver 구현 예정");
        };
    }

    private OAuthRegistration getRegistration(OAuthProvider provider) {
        return switch (provider) {
            case KAKAO -> OAuthRegistration.kakao();
            case NAVER -> OAuthRegistration.naver();
        };
    }

    // Oauth 페이지로 browser을 redirect하기 위한 요청 url 생성
    @Override
    public String getAuthorizeUrl(OAuthProvider provider) {
        // 등록/공급자 설정 가져오기
        OAuth2RegistrationPropertiesDto.RegistrationConfig registration =
                getRegistrationConfig(provider);
        OAuth2ProviderPropertiesDto.ProviderConfig providerConfig =
                getProviderConfig(provider);

        // 쿼리 파라미터 조립
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(providerConfig.authorizationUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", registration.clientId())
                .queryParam("redirect_uri", registration.redirectUri())
                .queryParam("state", UUID.randomUUID().toString());

        // scope가 있다면 공백 기준으로 합쳐서 추가
        if (registration.scope() != null && !registration.scope().isEmpty()) {
            builder.queryParam("scope", String.join(" ", registration.scope()));
        }

        return builder.build().toUriString();
    }

    // Authorization Code + client Secret을 조합한 값을 통해서 kakao Authorization Server에 값을 요청한다
    @Override
    public String getAccessToken(String code, OAuthProvider provider) {
        // 등록 정보 (client id, secret 등) 및 Oauth server url 정보 가져오기
        OAuth2RegistrationPropertiesDto.RegistrationConfig registration = getRegistrationConfig(provider);
        OAuth2ProviderPropertiesDto.ProviderConfig providerConfig = getProviderConfig(provider);

        // 요청 헤더 설정 (form-urlencoded)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 요청 바디 설정 (grant_type, client_id, redirect_uri, code, client_secret)
        String body = UriComponentsBuilder.newInstance()
                .queryParam("grant_type", "authorization_code")
                .queryParam("client_id", registration.clientId())
                .queryParam("redirect_uri", registration.redirectUri())
                .queryParam("code", code)
                .queryParam("client_secret", registration.clientSecret()) // ✅ client_secret 포함!
                .build()
                .toUri()
                .getRawQuery(); // form body용 query string 추출

        // 요청 객체 구성
        // Access-Token을 얻기 위한 요청은 Post요청이고 넣어야 하는 값이 많기에 RequestEntity보다 HttpEntity를 쓰는 것이 낫다
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            // POST 요청 수행
            ResponseEntity<Map> response = restTemplate.exchange(
                    providerConfig.tokenUri(), // ex) https://kauth.kakao.com/oauth/token
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();

            // access_token 추출
            if (responseBody != null && responseBody.containsKey("access_token")) {
                return responseBody.get("access_token").toString();
            } else {
                throw new RuntimeException("Access Token을 응답에서 찾을 수 없습니다: " + responseBody);
            }

        } catch (RestClientException e) {
            throw new RuntimeException("OAuth Access Token 요청 실패", e);
        }
    }

    // properties.oauth.yml에서 등록 정보 (client id, client secrert, redirect url)를 가져옴
    private OAuth2RegistrationPropertiesDto.RegistrationConfig getRegistrationConfig(OAuthProvider provider) {
        return switch (provider) {
            case KAKAO -> registrationProps.kakao();
            case NAVER -> registrationProps.naver();
        };
    }

    // properties.oauth.yml에서 Oauth server와 통신하기 위한 url 정보들을 가져옴
    private OAuth2ProviderPropertiesDto.ProviderConfig getProviderConfig(OAuthProvider provider) {
        return switch (provider) {
            case KAKAO -> providerProps.kakao();
            case NAVER -> providerProps.naver();
        };
    }
}
package com.C_platform.Member_woonkim.infrastructure.register_and_oauthClients;

import com.C_platform.Member_woonkim.application.port.OauthClientPort;
import com.C_platform.Member_woonkim.domain.enums.OAuthProvider;
import com.C_platform.Member_woonkim.exception.OauthErrorCode;
import com.C_platform.Member_woonkim.exception.OauthException;
import com.C_platform.Member_woonkim.infrastructure.dto.OAuth2ProviderPropertiesDto;
import com.C_platform.Member_woonkim.infrastructure.dto.OAuth2RegistrationPropertiesDto;
import com.C_platform.Member_woonkim.utils.OauthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NaverOAuthClient implements OauthClientPort {

    @Value("${app.base-url}") // application-oauth2-{환경 profils}.yml에서 값 가져옴
    private String baseUrl; // 예: https://api.your-domain.com/v1/

    private final OauthProperties oauthProperties; /// properties.oauth.yml을 읽어서 Dto로 반환 해주는 객체

    private final RestTemplate restTemplate; /// Request API와 관련된 Utils 객체

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.NAVER;
    }

    @Override
    // TODO :  MultiValueMap이 인코딩 안전성이 더 좋음 (권장).
    // Authorization Code + client Secret을 조합한 값을 통해서 naver Authorization Server에 값을 요청한다
    // stateCode : Authorization code / oauthCode : Authorize uri에 실었던 oauth state
    public String getAccessToken(String stateCode, String oauthCode, OAuthProvider provider) {
        // 설정 파일에 저장된 provider에 따른 oauth 관련 정보 가져옴
        OAuth2RegistrationPropertiesDto.RegistrationConfig registration = oauthProperties.getRegistrationConfig(provider); // clientId, redirectUri, scope(List<String>) 등
        OAuth2ProviderPropertiesDto.ProviderConfig providerConfig = oauthProperties.getProviderConfig(provider);   // authorizationUri 등

        // 요청 헤더 설정 (form-urlencoded)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 요청 바디 설정 (grant_type, client_id, redirect_uri, code, client_secret)
        String body = UriComponentsBuilder.newInstance()
                .queryParam("grant_type", "authorization_code")
                .queryParam("client_id", registration.clientId())
                .queryParam("redirect_uri", registration.redirectUri()) // 되돌아올 서버 url
                .queryParam("code", stateCode)
                .queryParam("state", oauthCode)
                .queryParam("client_secret", registration.clientSecret())
                .build()
                .toUri()
                .getRawQuery(); // form body용 query string 추출

        // 요청 객체 구성
        // Access-Token을 얻기 위한 요청은 Post요청이고 넣어야 하는 값이 많기에 RequestEntity보다 HttpEntity를 쓰는 것이 낫다
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        // 요청 보낸 뒤 응답에서 Access Token 꺼내서 반환
        try {
            // POST 요청 수행
            ResponseEntity<Map> response = restTemplate.exchange(
                    providerConfig.tokenUri(), // ex) https://kauth.kakao.com/oauth/token
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();

            // access_token 추출 후 반환
            if (responseBody != null && responseBody.containsKey("access_token")) {
                return responseBody.get("access_token").toString();
            } else {
                throw new OauthException(OauthErrorCode.C005);
            }
        } catch (RestClientException e) {
            throw new OauthException(OauthErrorCode.C005);
        }
    }

    @Override
    // *** Access-Token으로 Resource Server에 사용자 정보를 요청하여 받은 뒤 dto에 담아서 반환하는 method ***
    public Map<String, Object> getUserInfo(String accessToken, OAuthProvider provider) {
        OAuth2ProviderPropertiesDto.ProviderConfig providerConfig = oauthProperties.getProviderConfig(provider);

        // Resource Server에 보낼 요청 생성
        // Authorization Code를 Access-Token으로 교환할 때 client secret필요함 사용자 정보 단계에선 불필요
        RequestEntity<Void> request = RequestEntity
                .get(URI.create(providerConfig.userInfoUri()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .build();

        // Resource Server에서 받아올 사용자 정보 응답
        ResponseEntity<Map> response;

        try {
            // restTemplate.exchainge()가 실행되는 시점에 요청이 보내짐
            response = restTemplate.exchange(request, Map.class);
        } catch (RestClientException e) {
            throw new OauthException(OauthErrorCode.C006);
        }

        // response에 실린 사용자 정보 획득
        Map<String, Object> userInfoBody = response.getBody();

        // 응답 코드 확인 후 정상 코드인 "00" 아니면 에러 생성
        Object rc = userInfoBody.get("resultcode");
        if (rc != null && !"00".equals(rc.toString())) {
            throw new OauthException(OauthErrorCode.C006);
        }

        Object payload = userInfoBody.get("response");
        if (!(payload instanceof Map)) {
            throw new OauthException(OauthErrorCode.C007);
        }

        // parserRegistry는 provider에 따라 알맞은 Parser를 반환한다 (switch -> Registry 변경)
        return (Map<String, Object>) payload;
    }
}

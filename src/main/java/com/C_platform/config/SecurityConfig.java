package com.C_platform.config;

import com.C_platform.Member.domain.Oauth.CustomOAuth2UserService;
import com.C_platform.Member.domain.Oauth.OAuth2ProviderPropertiesDto;
import com.C_platform.Member.domain.Oauth.OAuth2RegistrationPropertiesDto;
import com.C_platform.Member.domain.Oauth.OAuth2Service;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({
        OAuth2RegistrationPropertiesDto.class,
        OAuth2ProviderPropertiesDto.class
})
public class SecurityConfig {

    // === 화이트리스트 ===
    private static final String[] SWAGGER_WHITELIST = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-resources/**"
    };

    private static final String[] AUTH_WHITELIST = {
            "/v1",
            "/v1/main",
            "/v1/auth/login",
            "/v1/auth/login/kakao",
            "/v1/auth/login/local"
    };

    // local login password 암호화 객체
    // Oauth에선 사용 안 함
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> cookieAuthorizationRequestRepository() {
        return new HttpSessionOAuth2AuthorizationRequestRepository();
    }

    @Bean
    // cors 설정 return
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(
                "https://your-frontend.com",
                "http://localhost:3000",
                // 프론트 서버 Origin 추가
                "https://carhartt-usedtransactions-frontend.pages.dev"
        ));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Content-Type", "X-Requested-With", "X-XSRF-TOKEN", "Authorization"));
        cfg.setAllowCredentials(true); // 세션/쿠키 쓰므로 true
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    // CookieCsrfTokenRepository는 lazy-write 이므로, GET 화면 진입 시 token.getToken()으로 쿠키 발급을 보장
    @Bean
    public XsrfPresenceFilter xsrfPresenceFilter() {
        return new XsrfPresenceFilter();
    }

    // java 코드로 쉽게 http 요청을 만들 수 있게 도와주는 객체
    // Oauth 통신 및 Server에서 API 호출을 할 때 자주 사용된다
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // todo 무슨 기능인지 공부 필요
    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService(OAuth2Service oauth2Service) {
        return new CustomOAuth2UserService(oauth2Service);
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            OAuth2RegistrationPropertiesDto registrationProps,
            OAuth2ProviderPropertiesDto providerProps
    ) {
        OAuth2RegistrationPropertiesDto.RegistrationConfig kakao = registrationProps.kakao();
        OAuth2ProviderPropertiesDto.ProviderConfig kakaoProvider = providerProps.kakao();

        ClientRegistration kakaoRegistration = ClientRegistration.withRegistrationId("kakao")
                .clientId(kakao.clientId())
                .clientSecret(kakao.clientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(kakao.redirectUri())
                .scope(kakao.scope().toArray(new String[0]))
                .authorizationUri(kakaoProvider.authorizationUri())
                .tokenUri(kakaoProvider.tokenUri())
                .userInfoUri(kakaoProvider.userInfoUri())
                .userNameAttributeName(kakaoProvider.userNameAttribute())
                .clientName(kakao.clientName())
                .build();

        return new InMemoryClientRegistrationRepository(kakaoRegistration);
    }

// ***************************************************************************************
// ************************************* filterChain main logic **************************
// ***************************************************************************************

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http, OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService) throws Exception {
    // cors
    http.cors(httpSecurityCorsConfigurer -> httpSecurityCorsConfigurer.configurationSource(corsConfigurationSource()));

    // CSRF (더블 서브밋: JS가 쿠키 XSRF-TOKEN을 읽어 X-XSRF-TOKEN 헤더로 반사)
    CookieCsrfTokenRepository repo =
            CookieCsrfTokenRepository.withHttpOnlyFalse();
    repo.setCookieName("XSRF-TOKEN");
    repo.setCookiePath("/");
    repo.setSecure(true);
    repo.setCookieCustomizer(c -> c.sameSite("Lax"));

    // csrf-token을 spring이 관리/검증하도록 인가
    http.csrf(csrf -> csrf.csrfTokenRepository(repo));

    // GET 진입 시 토큰 쿠키 보장
    http.addFilterAfter(xsrfPresenceFilter(), CsrfFilter.class);

    // 세션 관리 정책
    http.sessionManagement
            (httpSecuritySessionManagementConfigurer ->
            {
                httpSecuritySessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
            });

    // 인가(인가 규칙)
    http.authorizeHttpRequests(auth -> auth
            .requestMatchers(SWAGGER_WHITELIST).permitAll()
            .requestMatchers(AUTH_WHITELIST).permitAll()
            .anyRequest().authenticated()
    );

    // form기반이 아니라 json 기반 로컬 로그인이기에 아래 코드 사용 x
    // http.formLogin(AbstractHttpConfigurer::disable);

    // oauth 로그인 관련 지원
    http.oauth2Login(oauth2 -> {
        oauth2.authorizationEndpoint(authorization -> {
            authorization.authorizationRequestRepository(cookieAuthorizationRequestRepository());
        });

        oauth2.userInfoEndpoint(userInfo -> {
            // 아직 해당 객체 미구현
            userInfo.userService(customOAuth2UserService);
        });
    });

    return http.build();
}
}
package com.C_platform.config;

import com.C_platform.Member_woonkim.application.useCase.OAuth2UseCase;
import com.C_platform.Member_woonkim.domain.service.CustomOAuth2UserService;
import com.C_platform.Member_woonkim.infrastructure.dto.OAuth2ProviderPropertiesDto;
import com.C_platform.Member_woonkim.infrastructure.dto.OAuth2RegistrationPropertiesDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({
        OAuth2RegistrationPropertiesDto.class,
        OAuth2ProviderPropertiesDto.class
})
public class SecurityConfig {

    @Value("${app.identifier}")
    private String identifier;

    // === 화이트리스트 ===
    private static final String[] SWAGGER_WHITELIST = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-resources/**",
            "/h2-console/**" // H2 db를 test하기 위해 추가함
            // "/v1/**" // 로그인 기능을 구현 완료, 로그인 후 api 사용
    };

    // 현재 환경이 local인지 ec2인지 검사
    private boolean isLocal() {
        return "local".equalsIgnoreCase(identifier);
    }

    @Bean
    ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }

    // 수정: 콜백 경로 추가
    private static final String[] AUTH_WHITELIST = {
            "/v1",
            "/v1/main",
            "/v1/oauth/login",
            "/v1/oauth/login/kakao",
            "/v1/oauth/login/local",
            "/v1/oauth/kakao/callback"
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

    // cors 설정 return
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of( // cors 요청을 허용하는 origin들 목록
                "https://carhartt-usedtransactions.com",
                "http://localhost:3000",
                "http://localhost:8080",
                // 프론트 서버 Origin 추가 (5713 -> 5173 변경)
                "https://carhartt-usedtransactions-frontend.pages.dev",
                "https://carhartt-usedtransactions-frontend.pages.dev:5173",
                "http://localhost:5173"
        ));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
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
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService(OAuth2UseCase oAuth2UseCase) {
        return new CustomOAuth2UserService(oAuth2UseCase);
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
    repo.setSecure(!isLocal()); // 배포 환경에 따라 분기 (local - false / prod - true)
    repo.setCookieCustomizer(c -> c.sameSite("None"));

    // csrf-token을 spring이 관리/검증하도록 인가
    http.csrf(csrf -> csrf
             .csrfTokenRepository(repo)
            // todo 아직 csrf 비교 구현 전이라 해당 코드로 csrf 비교 껐음 (csrf 구현 후 지우기)
             .ignoringRequestMatchers(
                     "/v1/oauth/logout",
                     "/v1/oauth/login/check",
                     "/v1/myPage/**",
                     "/h2-console/**",
                     "/v1/orders/**"
             )
    );

    // H2 콘솔이 사용하는 프레임 기능 활성화
    http.headers(h -> h.frameOptions(f -> f.sameOrigin()));

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
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // 상태 변경 요청 전에 “프리플라이트(OPTIONS)에 대해 허용
            .requestMatchers(SWAGGER_WHITELIST).permitAll()
            .requestMatchers(AUTH_WHITELIST).permitAll()

            // 로그인 관련 허용 경로
            .requestMatchers("/v1/oauth/logout").permitAll()
            .requestMatchers("/v1/oauth/login/check").permitAll()

            // ✅ 주문 생성 API 실제 경로 허용
            .requestMatchers("/api/order").permitAll()

            // === 깡통 결제 API 전용 전체 허용 ===
            .requestMatchers("/v1/order/*/payment/**").permitAll()
            .requestMatchers("/v1/payment/**").permitAll()

            //마지막으로 anyRequest가 와야 함
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
package com.C_platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
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
                "http://localhost:3000"
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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
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
                userInfo.userService(oAuth2UserService);
            });
        });

        return http.build();
    }
}
package com.C_platform.Member_woonkim.infrastructure.auth.config;

import com.C_platform.Member_woonkim.domain.service.LocalUserDetailsService;
import com.C_platform.Member_woonkim.infrastructure.auth.filter.JsonUsernamePasswordAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Local 인증 설정
 *
 * - PasswordEncoder: BCrypt
 * - AuthenticationProvider: DaoAuthenticationProvider (UserDetailsService 기반)
 * - AuthenticationFilter: JsonUsernamePasswordAuthenticationFilter (JSON 요청 처리)
 */
@Configuration
@RequiredArgsConstructor
public class LocalAuthConfig {

    private final LocalUserDetailsService localUserDetailsService;
    private final ObjectMapper objectMapper;

    /**
     * PasswordEncoder Bean
     * BCrypt 알고리즘 사용 (strength = 10)
     *
     * @return BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * AuthenticationManager Bean
     *
     * @param authenticationConfiguration 인증 설정
     * @return AuthenticationManager
     * @throws Exception
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * DaoAuthenticationProvider Bean
     *
     * LocalUserDetailsService와 PasswordEncoder를 사용하여
     * 사용자 정보를 조회하고 비밀번호를 검증합니다.
     *
     * @return DaoAuthenticationProvider
     */
//    @Bean
//    public DaoAuthenticationProvider daoAuthenticationProvider() {
//        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
//        provider.setUserDetailsService(localUserDetailsService);
//        provider.setPasswordEncoder(passwordEncoder());
//        return provider;
//    }

    /**
     * JsonUsernamePasswordAuthenticationFilter Bean
     *
     * JSON 형식의 로그인 요청을 처리하는 필터입니다.
     * SecurityConfig에서 UsernamePasswordAuthenticationFilter 위치에 추가됩니다.
     *
     * @return JsonUsernamePasswordAuthenticationFilter
     */
    @Bean
    public JsonUsernamePasswordAuthenticationFilter jsonUsernamePasswordAuthenticationFilter(
            AuthenticationManager authenticationManager
    ) {
        JsonUsernamePasswordAuthenticationFilter filter = new JsonUsernamePasswordAuthenticationFilter(objectMapper);
        filter.setAuthenticationManager(authenticationManager);
        return filter;
    }
}

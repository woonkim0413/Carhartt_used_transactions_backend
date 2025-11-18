package com.C_platform.Member_woonkim.infrastructure.auth.filter;

import com.C_platform.Member_woonkim.presentation.dto.Local.request.LoginRequestDto;
import com.C_platform.Member_woonkim.utils.LogPaint;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextRepository;

import java.io.IOException;

/**
 * JSON 기반 Local 인증 필터
 *
 * UsernamePasswordAuthenticationFilter를 상속받아
 * form-data 대신 JSON 형식의 요청을 처리합니다.
 *
 * 요청: POST /v1/local/login
 * Body: { "email": "user@example.com", "password": "password123" }
 */
@Slf4j
public class JsonUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private static final String DEFAULT_LOGIN_URL = "/v1/local/login";
    private static final String DEFAULT_USERNAME_KEY = "email";
    private static final String DEFAULT_PASSWORD_KEY = "password";

    private final ObjectMapper objectMapper;

    public JsonUsernamePasswordAuthenticationFilter(
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        setFilterProcessesUrl(DEFAULT_LOGIN_URL);  // 처리할 URL 설정
        setUsernameParameter(DEFAULT_USERNAME_KEY);
        setPasswordParameter(DEFAULT_PASSWORD_KEY);
    }

    /**
     * HTTP 요청 본문에서 JSON을 파싱하여 인증 시도
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @return Authentication 객체 (인증 시도)
     * @throws AuthenticationException 인증 실패 시 발생
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        try {
            LogPaint.sep("login 처리 시작");

            // 1. 요청 본문을 JSON으로 파싱
            LoginRequestDto loginRequest = objectMapper.readValue(
                    request.getInputStream(),
                    LoginRequestDto.class
            );

            log.debug("JsonUsernamePasswordAuthenticationFilter: 요청 파싱 성공 - email: {}", loginRequest.getEmail());

            // 2. 이메일과 비밀번호 검증 및 정제 (널/공백 체크)
            String email = validateAndTrimEmail(loginRequest.getEmail());
            String password = validateAndTrimPassword(loginRequest.getPassword());

            log.debug("JsonUsernamePasswordAuthenticationFilter: 요청 검증 성공 - email: {}", email);

            // 3. UsernamePasswordAuthenticationToken 생성
            // principal = email (username 역할)
            // credentials = password
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            password
                    );

            log.debug("JsonUsernamePasswordAuthenticationFilter: 인증 토큰 생성 완료");

            // 4. AuthenticationManager가 authenticate() 호출
            // - loadUserByUsername(email) 호출 → LocalUserDetailsService
            // - PasswordEncoder.matches(password, encodedPassword) 검증
            // - 성공: Authentication(authenticated=true) 반환
            // - 실패: AuthenticationException 발생 → FailureHandler로 이동
            return this.getAuthenticationManager().authenticate(authToken);

        } catch (IOException e) {
            log.error("JsonUsernamePasswordAuthenticationFilter: JSON 파싱 실패", e);
            throw new AuthenticationServiceException("로그인 요청 본문을 읽을 수 없습니다", e);
        }
    }

    /**
     * 이메일 검증 및 정제
     * - null/blank 체크
     * - 앞뒤 공백 제거
     * - 기본 이메일 형식 검증 (@가 포함되어 있는지)
     *
     * @param email 파싱된 이메일 값
     * @return 정제된 이메일
     * @throws AuthenticationException 이메일이 유효하지 않은 경우
     */
    private String validateAndTrimEmail(String email) {
        if (email == null || email.isBlank()) {
            log.warn("JsonUsernamePasswordAuthenticationFilter: 이메일이 비어있음");
            throw new AuthenticationServiceException("이메일은 필수입니다");
        }

        String trimmedEmail = email.trim();

        // 기본 이메일 형식 검증 (@ 포함 여부)
        if (!trimmedEmail.contains("@")) {
            log.warn("JsonUsernamePasswordAuthenticationFilter: 유효하지 않은 이메일 형식 - {}", trimmedEmail);
            throw new AuthenticationServiceException("유효한 이메일 형식이어야 합니다");
        }

        return trimmedEmail;
    }

    /**
     * 비밀번호 검증 및 정제
     * - null/blank 체크
     * - 앞뒤 공백 제거
     *
     * @param password 파싱된 비밀번호 값
     * @return 정제된 비밀번호
     * @throws AuthenticationException 비밀번호가 유효하지 않은 경우
     */
    private String validateAndTrimPassword(String password) {
        if (password == null || password.isBlank()) {
            log.warn("JsonUsernamePasswordAuthenticationFilter: 비밀번호가 비어있음");
            throw new AuthenticationServiceException("비밀번호는 필수입니다");
        }
        return password.trim();
    }

    /**
     * 인증 성공 시 처리
     *
     * 🔧 수정된 SecurityContext 저장소 로직:
     * 1. SecurityContext 생성 및 Authentication 저장
     * 2. SecurityContextHolder에 저장 (현재 스레드)
     * 3. HttpSessionSecurityContextRepository를 사용하여 세션에 명시적으로 저장 ← 핵심!
     * 4. 부모 클래스의 처리도 실행 (호환성 보장)
     * 5. HttpSession 생성 (JSESSIONID 쿠키)
     * 6. SuccessHandler 호출 (JSON 응답)
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param chain FilterChain
     * @param authResult 인증된 Authentication 객체
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {
            // 🔧 Step 4: 부모 클래스의 표준 처리 실행 (SuccessHandler 호출)
            // 나머지는 기본 동작 그대로 (HttpSession 생성, JSESSIONID 쿠키 발급)
            // 추후 세션 저장소를 Tomcat 메모리 → Redis로 바꾸고 싶다면 의존성 및 설정만 추가하여 HttpSession의 구현체를 redis로 바꾸면 됨 (코드 수정 필요 x)
            super.successfulAuthentication(request, response, chain, authResult);

            log.info("JsonUsernamePasswordAuthenticationFilter.successfulAuthentication: 인증 성공 - email: {}",
                    authResult.getName());
            LogPaint.sep("login 처리 이탈");
        }

    /**
     * Content-Type 검사
     * JSON 요청인 경우만 이 필터가 처리하도록 함
     *
     * @param request HTTP 요청
     * @return JSON 요청이면 true, 아니면 false
     */
    @Override
    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
        if (!super.requiresAuthentication(request, response)) {
            return false;
        }

        String contentType = request.getContentType();
        boolean isJson = contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE);

        log.debug("JsonUsernamePasswordAuthenticationFilter: 요청 검사 - URL: {}, JSON: {}",
                request.getRequestURI(), isJson);

        return isJson;
    }
}

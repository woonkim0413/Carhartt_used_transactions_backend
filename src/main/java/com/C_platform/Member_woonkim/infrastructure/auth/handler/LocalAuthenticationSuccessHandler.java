package com.C_platform.Member_woonkim.infrastructure.auth.handler;

import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.domain.enums.LocalProvider;
import com.C_platform.Member_woonkim.infrastructure.db.MemberRepository;
import com.C_platform.Member_woonkim.presentation.dto.Local.response.LoginResponseDto;
import com.C_platform.Member_woonkim.utils.LogPaint;
import com.C_platform.global.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Local 인증 성공 핸들러
 *
 * 로그인 성공 시 호출되는 핸들러로, JSON 응답으로 회원 정보를 반환합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LocalAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;

    /**
     * 인증 성공 시 처리 로직
     *
     * 1. SecurityContext에 Authentication 저장 (세션 생성 트리거)
     * 2. HttpSession 명시적 생성
     * 3. 회원 정보 조회하여 JSON 응답으로 반환
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param authentication 인증 정보
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {
        log.info("LocalAuthenticationSuccessHandler.onAuthenticationSuccess: 로그인 성공 - email: {}",
                authentication.getName());

        // ✅ 주의: SecurityContext 저장 및 세션 생성은 JsonUsernamePasswordAuthenticationFilter.successfulAuthentication()에서 처리됨
        // 이 핸들러는 JSON 응답 반환만 담당

        // 1. 세션 ID 로깅 (디버깅 목적)
        String sessionId = request.getSession(true).getId();
        log.info("LocalAuthenticationSuccessHandler: 현재 JSESSIONID : {}", sessionId);

        // 2. 이메일 추출 (authentication.getName()에서 email 반환)
        String email = authentication.getName();

        // 3. Member 엔티티 조회
        Member member = memberRepository.findByLocalProviderAndEmail(LocalProvider.LOCAL, email)
                .orElseThrow(() -> {
                    log.error("LocalAuthenticationSuccessHandler: 인증된 사용자를 찾을 수 없음 - {}", email);
                    return new IllegalStateException("인증된 사용자를 찾을 수 없습니다");
                });

        log.debug("LocalAuthenticationSuccessHandler: 회원 정보 조회 성공 - memberId: {}", member.getMemberId());

        // 4. LoginResponseDto 생성
        LoginResponseDto responseDto = LoginResponseDto.from(member);

        // 5. ApiResponse로 감싸기
        com.C_platform.global.MetaData metaData = com.C_platform.global.MetaData.builder()
                .timestamp(java.time.LocalDateTime.now())
                .build();
        ApiResponse<LoginResponseDto> apiResponse = ApiResponse.success(responseDto, metaData);

        // 6. JSON 응답 설정 및 반환
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));

        log.info("LocalAuthenticationSuccessHandler: 로그인 응답 전송 완료");

        LogPaint.sep("login 처리 이탈");
    }
}

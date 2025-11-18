package com.C_platform.Member_woonkim.infrastructure.auth.handler;

import com.C_platform.global.ApiResponse;
import com.C_platform.global.MetaData;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Local 로그아웃 성공 핸들러
 *
 * 로그아웃 성공 시 호출되는 핸들러로, JSON 응답을 반환합니다.
 * 세션 무효화 등의 작업은 LogoutFilter가 자동으로 처리합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LocalLogoutSuccessHandler implements LogoutSuccessHandler {

    private final ObjectMapper objectMapper;

    /**
     * 로그아웃 성공 시 처리 로직
     * - 세션 무효화는 LogoutFilter에서 자동으로 처리됨
     * - 여기서는 JSON 응답만 반환
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param authentication 인증 정보
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                               Authentication authentication) throws IOException, ServletException {
        log.info("LocalLogoutSuccessHandler: 로그아웃 성공");

        // ApiResponse 생성 (성공, 데이터 없음)
        MetaData metaData = MetaData.builder()
                .timestamp(java.time.LocalDateTime.now())
                .build();

        ApiResponse<Void> apiResponse = ApiResponse.success(null, metaData);

        // JSON 응답 설정 및 반환
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.setStatus(HttpStatus.OK.value());
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));

        log.info("LocalLogoutSuccessHandler: 로그아웃 응답 전송 완료");
    }
}

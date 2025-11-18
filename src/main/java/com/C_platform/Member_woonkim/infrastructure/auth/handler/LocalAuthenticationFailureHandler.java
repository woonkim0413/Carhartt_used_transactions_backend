package com.C_platform.Member_woonkim.infrastructure.auth.handler;

import com.C_platform.global.ApiResponse;
import com.C_platform.Member_woonkim.exception.LocalAuthErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Local 인증 실패 핸들러
 *
 * 로그인 실패 시 호출되는 핸들러로, 적절한 에러 메시지를 반환합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LocalAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    /**
     * 인증 실패 시 처리 로직
     * - 예외 타입에 따라 적절한 에러 메시지 반환
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param exception 발생한 예외
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                       AuthenticationException exception) throws IOException, ServletException {
        log.warn("LocalAuthenticationFailureHandler: 로그인 실패 - 예외: {}", exception.getClass().getSimpleName());

        String errorCode = "C001";
        String errorMessage = "이메일 또는 비밀번호가 올바르지 않습니다";
        int httpStatus = HttpStatus.UNAUTHORIZED.value();

        // 예외 타입에 따라 분기 처리
        if (exception instanceof UsernameNotFoundException) {
            errorCode = "C002";
            errorMessage = "가입되지 않은 이메일입니다";
            log.warn("LocalAuthenticationFailureHandler: 사용자 미존재 - 요청 이메일");
        } else if (exception instanceof BadCredentialsException) {
            errorCode = "C003";
            errorMessage = "비밀번호가 올바르지 않습니다";
            log.warn("LocalAuthenticationFailureHandler: 비밀번호 오류");
        } else {
            log.warn("LocalAuthenticationFailureHandler: 기타 인증 오류 - {}", exception.getMessage());
        }

        // ApiResponse 생성
        com.C_platform.global.error.ErrorBody<LocalAuthErrorCode> errorBody = new com.C_platform.global.error.ErrorBody<>(
                errorCode,
                errorMessage
        );
        com.C_platform.global.MetaData metaData = com.C_platform.global.MetaData.builder()
                .timestamp(java.time.LocalDateTime.now())
                .build();
        ApiResponse<Void> errorResponse = ApiResponse.fail(errorBody, metaData);

        // JSON 응답 설정 및 반환
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.setStatus(httpStatus);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));

        log.info("LocalAuthenticationFailureHandler: 에러 응답 전송 완료 - errorCode: {}", errorCode);
    }
}

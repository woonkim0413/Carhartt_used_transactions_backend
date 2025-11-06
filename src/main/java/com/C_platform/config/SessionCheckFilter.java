package com.C_platform.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component // (1) 이 필터를 Spring 빈으로 등록합니다.
public class SessionCheckFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SessionCheckFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 필터 체인 실행 전에 세션 확인 (생성 X)1
        HttpSession sessionBefore = request.getSession(false);

        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);

        // 필터 체인 실행 후에 세션 다시 확인 (생성 X)
        HttpSession sessionAfter = request.getSession(false);

        // 이전에는 세션이 없었는데, 실행 후에 세션이 생겼다면 로그를 남김
        if (sessionBefore == null && sessionAfter != null) {
            log.info("[세션 생성됨!] URI: {}, New SessionID: {}", request.getRequestURI(), sessionAfter.getId());
        }
    }
}
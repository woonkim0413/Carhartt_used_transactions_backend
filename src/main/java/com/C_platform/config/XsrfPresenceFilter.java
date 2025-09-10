package com.C_platform.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class XsrfPresenceFilter extends OncePerRequestFilter {
    private static final AntPathMatcher PATHS = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        boolean candidate =
                "GET".equals(req.getMethod())
                        && (match(req, "/")
                        || match(req, "/auth/**")
                        || match(req, "/v1/auth/**")); // 필요 경로만 가드

        if (candidate) {
            CsrfToken token = (CsrfToken) req.getAttribute(CsrfToken.class.getName());
            if (token == null) {
                // Accessing token forces generation when using CookieCsrfTokenRepository
                token = (CsrfToken) req.getAttribute("_csrf");
            }
            // token.getToken()를 한번 호출해야 생성/쓰기 트리거가 걸림
            if (token != null) token.getToken(); // 존재 보장
        }
        chain.doFilter(req, res);
    }

    private boolean match(HttpServletRequest req, String pattern) {
        String path = req.getRequestURI();
        return PATHS.match(pattern, path);
    }
}

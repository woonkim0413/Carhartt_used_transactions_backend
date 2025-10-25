package com.C_platform.Member_woonkim.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ReqLogFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(ReqLogFilter.class);
    @Override protected void doFilterInternal(
            HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String cookies = req.getHeader("Cookie");

        log.info(">>> {} {} | Origin={} | Referer={} | Cookie={}",
                req.getMethod(),
                req.getRequestURI(),
                req.getHeader("Origin"),
                req.getHeader("Referer"),
                cookies != null ? cookies : "(쿠키 없음)"
        );
        chain.doFilter(req, res);
    }
}

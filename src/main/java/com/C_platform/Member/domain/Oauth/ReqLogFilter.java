package com.C_platform.Member.domain.Oauth;

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
        log.info(">>> {} {}  Referer={}  UA={}  Sec-Fetch-Mode={}  Sec-Fetch-Site={}  Sec-Purpose={}",
                req.getMethod(), req.getRequestURI(),
                req.getHeader("Referer"),
                req.getHeader("User-Agent"),
                req.getHeader("Sec-Fetch-Mode"),
                req.getHeader("Sec-Fetch-Site"),
                req.getHeader("Sec-Purpose"));
        chain.doFilter(req, res);
    }
}

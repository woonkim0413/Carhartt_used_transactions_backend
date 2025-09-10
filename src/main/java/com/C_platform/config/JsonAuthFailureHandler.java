package com.C_platform.config;

import jakarta.servlet.http.*;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import java.io.IOException;

public class JsonAuthFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest req, HttpServletResponse res,
                                        org.springframework.security.core.AuthenticationException ex) throws IOException {
        res.setStatus(401);
        res.setContentType("application/json");
        res.getWriter().write("""
        {"success":false,"error":{"code":"C002","message":"이메일 또는 비밀번호가 올바르지 않습니다."},"meta":{"request_id":"req_login_fail"}}""");
    }
}
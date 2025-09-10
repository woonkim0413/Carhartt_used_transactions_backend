package com.C_platform.config;

import jakarta.servlet.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import java.io.IOException;

public class JsonAuthSuccessHandler implements AuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication auth) throws IOException {
        res.setStatus(200);
        res.setContentType("application/json");
        res.getWriter().write("""
        {"success":true,"data":{"next_url":"/"},"meta":{"request_id":"req_login_ok"}}""");
    }
}
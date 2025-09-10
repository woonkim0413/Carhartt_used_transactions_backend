package com.C_platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class JsonUsernamePasswordAuthFilter {

    private final ObjectMapper om = new ObjectMapper();

    public JsonUsernamePasswordAuthFilter(
            String loginUrl,
            AuthenticationManager authenticationManager,
            org.springframework.security.web.authentication.AuthenticationSuccessHandler successHandler,
            org.springframework.security.web.authentication.AuthenticationFailureHandler failureHandler
    ) {
        super(new AntPathRequestMatcher(loginUrl, "POST"));
        setAuthenticationManager(authenticationManager);
        setAuthenticationSuccessHandler(successHandler);
        setAuthenticationFailureHandler(failureHandler);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {

        String ctype = request.getContentType();
        if (ctype == null || !ctype.toLowerCase().startsWith("application/json")) {
            throw new AuthenticationServiceException("Content-Type must be application/json");
        }

        String body = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        Map<String, Object> payload = om.readValue(body, Map.class);

        String email = String.valueOf(payload.getOrDefault("email", ""));
        String password = String.valueOf(payload.getOrDefault("password", ""));

        UsernamePasswordAuthenticationToken authRequest =
                new UsernamePasswordAuthenticationToken(email, password);

        authRequest.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return this.getAuthenticationManager().authenticate(authRequest);
    }
}

package com.C_platform.annotation;

import com.C_platform.Member_woonkim.domain.value.CustomLocalUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

public class WithMockCustomUserSecurityContextFactory
        implements WithSecurityContextFactory<WithMockCustomUser> {

    // param annotation : 사용자 정보를 갖고 있는 사용자 정의 에노테이션 (WithMockCustomUser)
    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
        // CustomLocalUser 생성
        CustomLocalUser customUser = CustomLocalUser.customBuilder()
                .username(annotation.email())
                .password(annotation.password())
                .authorities(Arrays.stream(annotation.roles())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList()))
                .accountExpired(annotation.accountExpired())
                .accountLocked(annotation.accountLocked())
                .credentialsExpired(annotation.credentialsExpired())
                .disabled(annotation.disabled())
                .memberId(annotation.memberId())
                .email(annotation.email())
                .localProvider(annotation.provider())
                .build();

        // Authentication 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                customUser,
                customUser.getPassword(),
                customUser.getAuthorities()
        );

        // SecurityContext에 설정
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);

        return context;
    }
}
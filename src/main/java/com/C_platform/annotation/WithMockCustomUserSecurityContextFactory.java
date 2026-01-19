package com.C_platform.annotation;

import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.domain.value.CustomLocalUser;
import com.C_platform.Member_woonkim.infrastructure.db.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @WithMockCustomUser 어노테이션을 처리하는 SecurityContextFactory
 *
 * 테스트 시 email로 DB에서 실제 Member를 조회하여 memberId를 동적으로 설정합니다.
 * DB에 해당 email의 Member가 없으면 어노테이션에 지정된 memberId를 fallback으로 사용합니다.
 */
@Component
@RequiredArgsConstructor
public class WithMockCustomUserSecurityContextFactory
        implements WithSecurityContextFactory<WithMockCustomUser> {

    private final MemberRepository memberRepository;

    // param annotation : 사용자 정보를 갖고 있는 사용자 정의 에노테이션 (WithMockCustomUser)
    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
        // email로 실제 Member 조회하여 memberId 획득 (없으면 annotation 값 사용)
        Long actualMemberId = memberRepository.findByEmail(annotation.email())
                .map(Member::getMemberId)
                .orElse(annotation.memberId()); // 기본 값 1

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
                .memberId(actualMemberId)  // DB에서 조회한 실제 memberId 사용
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
package com.C_platform.Member_woonkim.domain.entitys;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

// Spring Security가 인증을 끝낸 뒤 SecurityContext에 넣어두는 사용자 대표 객체이다
public class CustomOAuth2User implements OAuth2User {
    // principal로 사용할 객체
    private final Long memberId;
    private final Map<String, Object> attributes;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomOAuth2User(Long memberId,
                            Map<String, Object> attributes,
                            Collection<? extends GrantedAuthority> authorities) {
        this.memberId = memberId;
        this.attributes = attributes;
        this.authorities = authorities;
    }

    // CurrentMember에 사용됨
    public Long getMemberId() {
        return memberId;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return String.valueOf(memberId);
    }
}

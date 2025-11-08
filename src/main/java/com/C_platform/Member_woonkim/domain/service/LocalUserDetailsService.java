package com.C_platform.Member_woonkim.domain.service;

import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.domain.enums.LocalProvider;
import com.C_platform.Member_woonkim.infrastructure.db.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Local 인증 방식 사용자 조회 서비스
 * Spring Security의 UserDetailsService 인터페이스 구현
 *
 * 로그인 시 email을 username으로 사용하여 사용자 정보를 조회합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocalUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    /**
     * 이메일을 기반으로 사용자 정보를 조회하고 UserDetails로 변환
     *
     * @param email 사용자 이메일 (username 역할)
     * @return UserDetails 객체
     * @throws UsernameNotFoundException 사용자를 찾을 수 없을 때 발생
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("LocalUserDetailsService: 이메일로 사용자 조회 - {}", email);

        Member member = memberRepository.findByLocalProviderAndEmail(LocalProvider.LOCAL, email)
                .orElseThrow(() -> {
                    log.warn("LocalUserDetailsService: 사용자 찾기 실패 - {}", email);
                    return new UsernameNotFoundException("가입되지 않은 이메일입니다: " + email);
                });

        log.debug("LocalUserDetailsService: 사용자 조회 성공 - memberId: {}", member.getMemberId());

        // Member → UserDetails로 변환
        return User.builder()
                .username(member.getEmail())
                .password(member.getLoginPassword())  // DB에 저장된 BCrypt 암호화 비밀번호
                .authorities(Collections.emptyList())  // 역할 없음 (필요 시 추가)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}

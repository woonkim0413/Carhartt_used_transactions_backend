package com.C_platform.Member_woonkim.application.service;

import com.C_platform.Member_woonkim.domain.Oauth.JoinOrLoginResult;
import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.domain.enums.LocalProvider;
import com.C_platform.Member_woonkim.domain.enums.OAuthProvider;
import com.C_platform.Member_woonkim.infrastructure.db.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberJoinService {

    private final MemberRepository memberRepository;

    @Transactional
    // db에 (provider + oauthId) unique key 기반으로 member가 찾아지면 map 내부 lambda 실행, 못 찾으면 orEseGet 내부 lambda 실행
    public JoinOrLoginResult ensureOAuthMember(OAuthProvider provider, String oauthId, String name, String email) {
        return memberRepository.findByOauthProviderAndOauthId(provider, oauthId)
                .map(found -> new JoinOrLoginResult(found, false))
                .orElseGet(() -> new JoinOrLoginResult(createOAuthMember(provider, oauthId, name, email), true));
    }

    @Transactional
    // local 기반으로 회원가입을 한 member가 있는지 검사
    public JoinOrLoginResult ensureLocalMember(LocalProvider localProvider, String email, String encodedPassword, String name) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required for LOCAL");
        }
        return memberRepository.findByLocalProviderAndEmail(localProvider, email)
                .map(found -> new JoinOrLoginResult(found, false))
                .orElseGet(() -> new JoinOrLoginResult(createLocalMember(localProvider, email, encodedPassword, name), true));
    }

    // ensureOAuthMember에서 unique key로 회원가입된 member을 찾는데 실패하면 해당 메서드로 멤버 생성하여 db에 저장
    private Member createOAuthMember(OAuthProvider provider, String oauthId, String name, String email) {
        try {
            Member m = new Member(provider, oauthId, name, email); // ← 생성자 사용
            return memberRepository.save(m);
        } catch (DataIntegrityViolationException e) {
            return memberRepository.findByOauthProviderAndOauthId(provider, oauthId)
                    .orElseThrow(() -> e);
        }
    }

    private Member createLocalMember(LocalProvider localProvider, String email, String encodedPassword, String name) {
        try {
            Member m = new Member(localProvider, email, encodedPassword, name); // ← 생성자 사용
            return memberRepository.save(m);
        } catch (DataIntegrityViolationException e) {
            return memberRepository.findByLocalProviderAndEmail(localProvider, email)
                    .orElseThrow(() -> e);
        }
    }
}
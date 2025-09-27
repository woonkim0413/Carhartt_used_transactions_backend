package com.C_platform.Member.domain.Oauth;

import com.C_platform.Member.domain.Member.Member;
import com.C_platform.Member.infrastructure.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberJoinService {

    private final MemberRepository memberRepository;

    @Transactional
    public JoinOrLoginResult ensureOAuthMember(OAuthProvider provider, String oauthId, String name, String email) {
        return memberRepository.findByOauthProviderAndOauthId(provider, oauthId)
                .map(found -> new JoinOrLoginResult(found, false))
                .orElseGet(() -> new JoinOrLoginResult(createOAuthMember(provider, oauthId, name, email), true));
    }

    @Transactional
    public JoinOrLoginResult ensureLocalMember(LocalProvider localProvider, String email, String encodedPassword, String name) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required for LOCAL");
        }
        return memberRepository.findByLocalProviderAndEmail(localProvider, email)
                .map(found -> new JoinOrLoginResult(found, false))
                .orElseGet(() -> new JoinOrLoginResult(createLocalMember(localProvider, email, encodedPassword, name), true));
    }

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
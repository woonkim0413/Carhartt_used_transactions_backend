package com.C_platform.Member_woonkim.application.useCase;

import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.infrastructure.db.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyPageUseCase {

    private final MemberRepository memberRepository;

    @Transactional
    public Long changeMemberNickname(String newNickname, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException());

        member.changeNickname(newNickname);

        return member.getMemberId();
    }
}


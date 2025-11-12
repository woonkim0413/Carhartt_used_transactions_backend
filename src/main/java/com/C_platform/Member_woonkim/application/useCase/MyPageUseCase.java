package com.C_platform.Member_woonkim.application.useCase;

import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.infrastructure.db.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MyPageUseCase {

    private final MemberRepository memberRepository;

    @Transactional
    public Long changeMemberNickname(String newNickname, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException());

        log.info("[디버깅 목적] 변경 전 닉네임 : {} / 변경 후 닉네임 : {}",member.getNickname() ,newNickname);

        member.changeNickname(newNickname);

        return member.getMemberId();
    }

    @Transactional
    public void updateProfileImage(Long memberId, String profileImageUrl) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        member.changeProfileImage(profileImageUrl);
    }
}


package com.C_platform.Member_woonkim.application.useCase;

import com.C_platform.Member_woonkim.application.port.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MyPageUseCase {

    private final MemberService memberService;

    public Long changeMemberNickname(String newNickname, Long memberId) {
        Long returndMemberId = memberService.changeMemberNicKname(newNickname, memberId);
        return returndMemberId;
    }
}


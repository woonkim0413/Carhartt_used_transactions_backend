package com.C_platform.Member_woonkim.application.useCase;

import com.C_platform.Member_woonkim.application.port.MemberServicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MyPageUseCase {

    private final MemberServicePort memberServicePort;

    public Long changeMemberNickname(String newNickname, Long memberId) {
        Long returndMemberId = memberServicePort.changeMemberNicKname(newNickname, memberId);
        return returndMemberId;
    }
}


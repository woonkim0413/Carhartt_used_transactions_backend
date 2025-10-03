package com.C_platform.Member_woonkim.application.port;

import com.C_platform.Member_woonkim.domain.entitys.Member;

public interface MemberService {
    // Member getMember();
    // Member saveMember(Member member);
    // Member deleteMember(Member member);
    Long changeMemberNicKname(String newNickname, Long memberId);
}

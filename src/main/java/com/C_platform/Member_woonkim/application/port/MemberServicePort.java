package com.C_platform.Member_woonkim.application.port;

public interface MemberServicePort {
    // Member getMember();
    // Member saveMember(Member member);
    // Member deleteMember(Member member);
    Long changeMemberNicKname(String newNickname, Long memberId);
}

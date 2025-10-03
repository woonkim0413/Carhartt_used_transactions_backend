package com.C_platform.Member_woonkim.infrastructure.adapter;

import com.C_platform.Member_woonkim.application.port.MemberService;
import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.infrastructure.db.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public Long changeMemberNicKname(String newNickname, Long memberId) {
        Optional<Member> memberOptional = memberRepository.findById(memberId);
        Member member = (memberOptional.isPresent()) ? memberOptional.get() : null;

        member.changeNickname(newNickname);

        return member.getMemberId();
    }
}

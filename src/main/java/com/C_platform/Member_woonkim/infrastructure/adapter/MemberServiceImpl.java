package com.C_platform.Member_woonkim.infrastructure.adapter;

import com.C_platform.Member_woonkim.application.port.MemberServicePort;
import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.infrastructure.db.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
// TODO : Repo가 UseCase에 위치하고 Service는 Member를 arg로 받아서 처리하도록 구조 변경하기
public class MemberServiceImpl implements MemberServicePort {

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

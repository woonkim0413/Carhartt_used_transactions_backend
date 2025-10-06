package com.C_platform.Member_woonkim.presentation.Assembler;

import com.C_platform.Member_woonkim.presentation.dto.myPage.response.ChangeNicknameResponseDto;
import org.springframework.stereotype.Component;

@Component
public class MyPageAssembler {
    public ChangeNicknameResponseDto createChangeNicknameResponseDto (
            Long memberId,
            String nickname
    ) {
        return ChangeNicknameResponseDto.builder()
                .memberId(memberId)
                .nickname(nickname)
                .build();
    }
}

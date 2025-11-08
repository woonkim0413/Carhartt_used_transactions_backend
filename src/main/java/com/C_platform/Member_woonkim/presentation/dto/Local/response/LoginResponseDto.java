package com.C_platform.Member_woonkim.presentation.dto.Local.response;

import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.domain.enums.LoginType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDto {

    private Long memberId;
    private String email;
    private String name;
    private String nickname;
    private LoginType loginType;

    public static LoginResponseDto from(Member member) {
        return LoginResponseDto.builder()
                .memberId(member.getMemberId())
                .email(member.getEmail())
                .name(member.getName())
                .nickname(member.getNickname())
                .loginType(member.getLoginType())
                .build();
    }
}

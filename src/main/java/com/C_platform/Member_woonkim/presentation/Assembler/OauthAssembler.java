package com.C_platform.Member_woonkim.presentation.Assembler;

import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.infrastructure.dto.OAuth2UserInfoDto;
import com.C_platform.Member_woonkim.presentation.dto.Oauth.response.CallBackResponseDto;
import com.C_platform.Member_woonkim.presentation.dto.Oauth.response.LogoutResponseDto;
import com.C_platform.Member_woonkim.presentation.dto.Oauth.response.RedirectToKakaoResponseDto;
import jakarta.servlet.http.HttpSession;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Component
public class OauthAssembler {
    public RedirectToKakaoResponseDto getRedirectToKakaoResponseDto(String authorizeUrl) {
        return RedirectToKakaoResponseDto.builder()
                .authorizeKakaoUrl(authorizeUrl)
                .build();
    }

    public CallBackResponseDto getCallBackResponseDto(
            HttpSession session,
            OAuth2UserInfoDto userInfo
    ) {
        return CallBackResponseDto.builder()
                .sessionId(session.getId())
                .user(
                        CallBackResponseDto.User.builder()
                                .id(String.valueOf(userInfo.getId()))
                                .name(userInfo.getName())
                                .email(userInfo.getEmail())
                                .build()
                )
                .build();
    }

    public LogoutResponseDto getLogoutResponseDto(String logout_message) {
        return LogoutResponseDto.builder()
                .logout_message(logout_message)
                .build();
    }
}

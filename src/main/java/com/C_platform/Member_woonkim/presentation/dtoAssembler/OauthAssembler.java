package com.C_platform.Member_woonkim.presentation.dtoAssembler;

import com.C_platform.Member_woonkim.infrastructure.dto.OAuth2UserInfoDto;
import com.C_platform.Member_woonkim.presentation.dto.Oauth.response.CallBackResponseDto;
import com.C_platform.Member_woonkim.presentation.dto.Oauth.response.LogoutResponseDto;
import com.C_platform.Member_woonkim.presentation.dto.Oauth.response.CreateRedirectUriResponseDto;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class OauthAssembler {
    public CreateRedirectUriResponseDto getCreateRedirectUriResponseDto(String authorizeUrl) {
        return CreateRedirectUriResponseDto.builder()
                .authorizeUrl(authorizeUrl)
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

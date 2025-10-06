package com.C_platform.Member_woonkim.presentation.Assembler;

import com.C_platform.Member_woonkim.presentation.dto.Oauth.response.RedirectToKakaoResponseDto;
import org.springframework.stereotype.Component;

@Component
public class OauthAssembler {
    public RedirectToKakaoResponseDto getRedirectToKakaoResponseDto(String authorizeUrl) {
        return RedirectToKakaoResponseDto.builder()
                .authorizeKakaoUrl(authorizeUrl)
                .build();
    }
}

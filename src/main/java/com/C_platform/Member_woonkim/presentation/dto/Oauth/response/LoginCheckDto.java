package com.C_platform.Member_woonkim.presentation.dto.Oauth.response;

import com.C_platform.Member_woonkim.domain.enums.LoginType;
import com.C_platform.Member_woonkim.domain.interfaces.Provider;
import lombok.Builder;

@Builder
public record LoginCheckDto(
    Long memberId,
    String memberName,
    String memberNickname,
    String loginType,
    String provider
) {}

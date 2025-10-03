package com.C_platform.Member_woonkim.presentation.dto;

import com.C_platform.Member_woonkim.domain.enums.LoginType;
import com.C_platform.Member_woonkim.domain.interfaces.Provider;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class LoginProviderDto {
    @Schema(description = "로그인 식별자", example = "KAKAO")
    private Provider provider;

    @Schema(description = "로그인 식별자", example = "KAKAO")
    @JsonProperty("type")
    private LoginType loginType;

    @Schema(description = "해당 provider redirect url 생성", example = "http://43.203.218.247:8080//v1/auth/login/kakao")
    @JsonProperty("authorize_url")
    private String authorizeUrl;
}

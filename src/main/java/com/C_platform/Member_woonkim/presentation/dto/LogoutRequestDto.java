package com.C_platform.Member_woonkim.presentation.dto;

import com.C_platform.Member_woonkim.domain.enums.LoginType;
import com.C_platform.Member_woonkim.domain.enums.OAuthProvider;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;

// @Builder
@Getter
@NoArgsConstructor
public class LogoutRequestDto {
    // enum : LOCAL, OAUTH
    @JsonProperty("type")
    @Schema(description = "타입", example = "OAUTH")
    @NotNull
    private LoginType type;


    // enum : KAKAO, NAVER, LOCALL
    @JsonProperty("provider")
    @Schema(description = "로그인 방법", example = "KAKAO")
    @NotNull
    // Provider을 넣으니 inteferface라 터짐 근데 그러면 Local은 어떻게 받아야 하지?
    private OAuthProvider provider;
}

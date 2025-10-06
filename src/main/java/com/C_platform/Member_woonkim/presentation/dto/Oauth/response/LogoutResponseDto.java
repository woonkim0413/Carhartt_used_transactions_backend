package com.C_platform.Member_woonkim.presentation.dto.Oauth.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record LogoutResponseDto (
    @JsonProperty("logout_message")
    @Schema(description = "로그인 성공 유무")
    String logout_message
) {}

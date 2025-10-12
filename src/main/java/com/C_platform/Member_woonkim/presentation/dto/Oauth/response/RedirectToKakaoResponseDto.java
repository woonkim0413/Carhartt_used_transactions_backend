package com.C_platform.Member_woonkim.presentation.dto.Oauth.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record RedirectToKakaoResponseDto (
        @JsonProperty("authorize_kakao_url")
        @Schema(description = "Oauth 승인 URL")
        String authorizeKakaoUrl
) {}

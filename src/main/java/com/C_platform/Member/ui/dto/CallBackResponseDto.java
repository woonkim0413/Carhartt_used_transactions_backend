package com.C_platform.Member.ui.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "AuthLoginData")
public class CallBackResponseDto {

    @Schema(description = "로그인한 사용자 요약 정보")
    private final User user;

    @JsonProperty("session_id")
    @Schema(description = "유저 정보 세션 식별자")
    private final String sessionId;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class User {
        @Schema(description = "OAuth 리소스 식별자", example = "1234567890")
        private final String id;

        @Schema(description = "표시 이름", example = "김운강")
        private final String name;

        @Schema(description = "이메일", example = "user@example.com")
        private final String email;
    }
}

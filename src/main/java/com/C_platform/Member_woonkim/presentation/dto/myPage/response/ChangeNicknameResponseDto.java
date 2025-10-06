package com.C_platform.Member_woonkim.presentation.dto.myPage.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record ChangeNicknameResponseDto(
        @JsonProperty("member_id")
        @Schema(description = "멤버ID", example = "1")
        Long memberId,

        @JsonProperty("nickname")
        @Schema(description = "변경된 멤버 닉네임", example = "회사가기싫엉")
        String nickname
)
{}

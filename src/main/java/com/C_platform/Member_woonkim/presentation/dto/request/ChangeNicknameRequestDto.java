package com.C_platform.Member_woonkim.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangeNicknameRequestDto(
        @JsonProperty("nickname")
        @NotBlank(message = "닉네임은 비워둘 수 없습니다.")
        @Size(max = 20, message = "닉네임은 최대 {max}자까지 가능합니다.")
        @Pattern(regexp = "^[a-zA-Z0-9가-힣_.-]+$", message = "닉네임은 한글/영문/숫자/._- 만 허용됩니다.")
        @Schema(description = "닉네임", example = "회사가기싫엉")
        String changeNickname
) {}

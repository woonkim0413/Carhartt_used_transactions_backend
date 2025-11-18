package com.C_platform.Member_woonkim.presentation.dto.Local.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RandomCodeVerificationDto(
        @NotBlank(message = "인증할 email을 입력해 주세요")
        @Size(max = 150, message = "email이 너무 깁니다")
        @Schema(description = "email", example = "dnsrkd0414@naver.com")
        @Pattern(regexp = ".*@.*", message = "올바른 email을 입력해 주세요")
        String email,

        @NotBlank(message = "인증 코드를 입력해주세요")
        @Schema(description = "6자리 인증 코드", example = "123456")
        String code
) {}

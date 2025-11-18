package com.C_platform.Member_woonkim.presentation.dto.Local.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordFindRequestDto(
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "유효한 이메일 형식이 아닙니다")
        @Size(max = 150, message = "email이 너무 깁니다")
        @Schema(description = "email", example = "user@example.com")
        String email
) {}

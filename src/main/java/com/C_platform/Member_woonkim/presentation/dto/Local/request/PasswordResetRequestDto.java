package com.C_platform.Member_woonkim.presentation.dto.Local.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetRequestDto(
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "유효한 이메일 형식이 아닙니다")
        @Size(max = 150, message = "email이 너무 깁니다")
        @Schema(description = "email", example = "user@example.com")
        String email,

        @NotBlank(message = "인증 코드는 필수입니다")
        @Size(min = 6, max = 6, message = "인증 코드는 6자리입니다")
        @Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 숫자만 포함해야 합니다")
        @Schema(description = "verification code", example = "123456")
        String code,

        @NotBlank(message = "새 비밀번호는 필수입니다")
        @Size(min = 8, max = 50, message = "비밀번호는 8~50자여야 합니다")
        @Schema(description = "new password", example = "NewPassword123!")
        String newPassword
) {}

package com.C_platform.Member_woonkim.presentation.dto.Local.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupRequestDto {

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이어야 합니다")
    @Schema(description = "email", example = "dnsrkd0414@naver.com")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Length(min = 8, max = 50, message = "비밀번호는 8~50자여야 합니다")
    @Schema(description = "password", example = "gjsxjsms123!")
    private String password;

    @NotBlank(message = "이름은 필수입니다")
    @Length(min = 2, max = 50, message = "이름은 2~50자여야 합니다")
    @Schema(description = "userName", example = "김운강")
    private String name;
}

package com.C_platform.Member_woonkim.presentation.dto.Local.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RandomCodeVerificationDto(
        @NotBlank(message = "생성한 난수를 입력해주세요")
        @Schema(description = "생성한 난수", example = "xxxxxxx")
        @JsonProperty("random_code")
        String randomCode
) {}

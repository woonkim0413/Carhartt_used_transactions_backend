package com.C_platform.Member_woonkim.presentation.dto.myPage.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileImageRequestDto(
        @NotBlank(message = "프로필 이미지 URL은 비어 있을 수 없습니다.")
        String profileImageUrl
) {
}

package com.C_platform.Member_woonkim.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

// todo : 고도화 할 때 해당 영역에서 예외 터지면 그에 해당하는 예외 반환 생성하기
public record KakaoCallbackRequestDto (
        @NotBlank String code,
        @NotBlank String state
) {}


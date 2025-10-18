package com.C_platform.payment.ui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record CompletePaymentRequest(
        @NotBlank(message = "provider는 필수입니다")
        String provider,      // "kakao", "naver"

        @NotBlank(message = "pgToken은 필수입니다")
        String pgToken        // 카카오페이에서 발급한 토큰
) {}
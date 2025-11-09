package com.C_platform.payment.ui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CompletePaymentRequest(

        @JsonProperty("order_id")
        @NotNull(message = "orderId는 필수입니다")
        Long orderId, //추가

        @NotBlank(message = "provider는 필수입니다")
        String provider,      // "kakao", "naver"

        @NotBlank(message = "pgToken은 필수입니다")
        String pgToken        // 카카오페이에서 발급한 토큰
) {}
package com.C_platform.payment.ui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record CompletePaymentRequest(
        @JsonProperty("provider")      @NotBlank String provider,           // KAKAOPAY | NAVERPAY
        @JsonProperty("order_id")  @NotBlank Long orderId,        // 예: order-202-20250905-001
        @JsonProperty("pg_token")      String pgToken                      // 성공 시 값, 실패/취소면 null
        // SUCCESS | FAIL | CANCEL
) {}

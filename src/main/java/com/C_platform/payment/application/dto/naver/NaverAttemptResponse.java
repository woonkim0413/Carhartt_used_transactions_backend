package com.C_platform.payment.application.dto.naver;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record NaverAttemptResponse(
        @JsonProperty("reserveId") String reserveId,
        @JsonProperty("paymentUrl") PaymentUrl paymentUrl
) {
    @Builder
    public record PaymentUrl(
            @JsonProperty("pc") String pc,
            @JsonProperty("mobile") String mobile
    ) {}
}

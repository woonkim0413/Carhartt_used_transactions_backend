package com.C_platform.payment.application.dto.naver;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record NaverCompleteResponse(
        @JsonProperty("code") String code,
        @JsonProperty("message") String message,
        @JsonProperty("body") Body body
) {
    @Builder
    public record Body(
            @JsonProperty("paymentId") String paymentId,
            @JsonProperty("merchantPayKey") String merchantPayKey,
            @JsonProperty("totalPayAmount") int totalPayAmount
    ) {}
}




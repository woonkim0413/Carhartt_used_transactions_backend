package com.C_platform.payment.application.dto.naver;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NaverCompleteRequest(
        @JsonProperty("paymentId") String paymentId
) {}

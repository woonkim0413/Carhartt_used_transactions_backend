package com.C_platform.payment.ui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AttemptPaymentResponse(
        @JsonProperty("redirect_url") String redirectUrl
) {}

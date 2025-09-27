package com.C_platform.payment.ui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CompletePaymentResponse(
        @JsonProperty("order_id") Long orderId
) {}

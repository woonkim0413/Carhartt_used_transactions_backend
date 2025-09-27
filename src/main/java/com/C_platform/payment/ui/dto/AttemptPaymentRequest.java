package com.C_platform.payment.ui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AttemptPaymentRequest(

        @JsonProperty("order_id")         @NotNull Long orderId,
        @JsonProperty("payment_method")   @NotBlank String paymentMethod, // "KAKAOPAY"
        @JsonProperty("amount_of_payment")@NotNull @Positive
Integer amountOfPayment,
        @JsonProperty("approve_url")      @NotBlank String approveUrl,
        @JsonProperty("fail_url")         @NotBlank String failUrl,
        @JsonProperty("cancel_url")       @NotBlank String cancelUrl
) {}

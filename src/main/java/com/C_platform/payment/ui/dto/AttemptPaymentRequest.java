package com.C_platform.payment.ui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AttemptPaymentRequest(

        @JsonProperty("order_id")         @NotNull Long orderId, //주문 ID
        @JsonProperty("payment_method")   @NotBlank String paymentMethod, // "KAKAOPAY" or "NAVERPAY"
        @JsonProperty("amount_of_payment")@NotNull @Positive
Integer amountOfPayment, //결제 금액
        @JsonProperty("approve_url")      @NotBlank String approveUrl, // 카카오: 승인 콜백 | 네이버: returnUrl
        @JsonProperty("fail_url")         @NotBlank String failUrl, // 결제 취소 시 리다이렉트 url
        @JsonProperty("cancel_url")       @NotBlank String cancelUrl // 결제 실패 시 리다이렉트 url


) {
    /**
     * 네이버페이 전용 변환 편의 메서드
     * 네이버의 returnUrl 필드로 approveUrl을 매핑할 때 사용
     */
    public String returnUrl() {
        return approveUrl;
    }
}

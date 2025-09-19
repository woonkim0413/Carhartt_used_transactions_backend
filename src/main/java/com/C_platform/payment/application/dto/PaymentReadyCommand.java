package com.C_platform.payment.application.dto;

public record PaymentReadyCommand(
        Long orderId,
        String paymentMethod // "KAKAOPAY" | "NAVERPAY"
) { }

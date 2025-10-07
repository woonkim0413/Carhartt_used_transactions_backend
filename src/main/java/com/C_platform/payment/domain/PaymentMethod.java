package com.C_platform.payment.domain;

public enum PaymentMethod {
    KAKAOPAY, NAVERPAY;

    public static PaymentMethod from(String s) {
        return PaymentMethod.valueOf(s.trim().toUpperCase());
    }
}

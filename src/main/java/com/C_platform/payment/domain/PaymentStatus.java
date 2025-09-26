package com.C_platform.payment.domain;

public enum PaymentStatus {
    READY,      // 결제 전
    PENDING,    // PG 요청 중 (세션 생성, redirect_url 발급됨)
    PAID,       // 결제 성공 + 승인 완료
    CANCELED,   // PG 취소 or 실패
}

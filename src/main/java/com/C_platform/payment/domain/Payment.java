package com.C_platform.payment.domain;

import com.C_platform.order.domain.Order;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment")
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "transaction_id", length = 255) // 오타 수정
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20, nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 20)
    private PaymentStatus paymentStatus;

    @Column(name = "amount_of_payment", nullable = false)
    private int amountOfPayment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "tid", length = 100)
    private String tid;

    @Column(name = "approve_url", length = 500)
    private String approveUrl;

    @Column(name = "fail_url", length = 500)
    private String failUrl;

    @Column(name = "cancel_url", length = 500)
    private String cancelUrl;

    // --- 라이프사이클 ---
    @PrePersist
    void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.paymentStatus == null) this.paymentStatus = PaymentStatus.READY;
    }

    // --- 팩토리 ---
    public static Payment newReady(Order order, PaymentMethod method, int amount) {
        return Payment.builder()
                .order(order)
                .paymentMethod(method)
                .amountOfPayment(amount)
                .paymentStatus(PaymentStatus.READY)
                .build();
    }

    // --- 상태 전이 ---
    public void markPending(String tid) {
        this.transactionId = tid;
        this.paymentStatus = PaymentStatus.PENDING;
    }

    public void approve() {
        this.paymentStatus = PaymentStatus.PAID;
        this.approvedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.paymentStatus = PaymentStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }

    @Deprecated
    public void markRedirected(String tid) {
        markPending(tid);
    }

    @Deprecated
    public void fail() {
        cancel();
    }
}


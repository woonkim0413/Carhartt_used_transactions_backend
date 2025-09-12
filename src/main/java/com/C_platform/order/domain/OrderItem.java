package com.C_platform.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.Entity;

//주문 시점 상품 복사
@Entity
@Table(
        name = "order_item",
        indexes = {
                @Index(name = "ix_order_item_order_id", columnList = "order_id"),
                @Index(name = "ix_order_item_item_id", columnList = "item_id") // 원본 item 추적용(참조용 값)
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    //스냅샷 필드
    @Column(name = "order_item_name", length = 120, nullable = false)
    private String orderItemName;

    @Column(name = "unit_price", nullable = false)
    private Long unitPrice; //KRW 정수

    //할인율 존재 유무 판단 필요
    //@Column(name = "count", nullable = false)
    //private Long count;

    //추적용 값들(FK 걸지 말 것)
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    // --- 생성 전용 팩토리 ---
    private OrderItem(Long orderId, Long itemId, String orderItemName, Long unitPrice) {
        if (unitPrice == null || unitPrice < 0)
            throw new IllegalArgumentException("unitPrice invalid");
        this.orderId = orderId;
        this.itemId = itemId;
        this.orderItemName = orderItemName;
        this.unitPrice = unitPrice;
    }

    public static OrderItem snapshotOf(Long orderId, Long itemId, String itemName, Long unitPrice) {
        return new OrderItem(orderId, itemId, itemName, unitPrice);
    }

    // --- 도메인 검증 메서드 ---
    /** 결제 승인 금액 검증 */
    public void assertApprovedAmount(Long approvedAmount) {
        if (!this.unitPrice.equals(approvedAmount)) {
            throw new IllegalStateException("approved amount mismatch");
        }
    }

    /** 소속 주문 일치 검증 */
    public void assertBelongsTo(Long expectedOrderId) {
        if (!this.orderId.equals(expectedOrderId)) {
            throw new IllegalStateException("order ownership mismatch");
        }
    }
}

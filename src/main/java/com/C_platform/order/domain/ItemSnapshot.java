package com.C_platform.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemSnapshot {

    /** 주문 시점의 원본 상품 식별자(참조용 값, FK 강제 X) */
    @Column(name = "snapshot_item_id", nullable = false)
    private Long itemId;

    /** 주문 시점 상품명 */
    @Column(name = "snapshot_item_name")  // ← 추가
    private String itemName;

    /** 주문 시점 단가 */
    @Column(name = "snapshot_item_price", nullable = false)
    private Integer price;

    private ItemSnapshot(Long itemId, String itemName, Integer price) {
        if (itemId == null) throw new IllegalArgumentException("itemId required");
        if (price == null || price < 0) throw new IllegalArgumentException("price must be >= 0");
        this.itemId = itemId;
        this.itemName = itemName;  // ← 추가
        this.price = price;
    }

    public static ItemSnapshot of(Long itemId, String itemName, Integer price) {
        return new ItemSnapshot(itemId, itemName, price);
    }
}


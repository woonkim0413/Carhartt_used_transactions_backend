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

    /** 주문 시점 단가 */
    @Column(name = "snapshot_item_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    private ItemSnapshot(Long itemId, BigDecimal price) {
        if (itemId == null) throw new IllegalArgumentException("itemId required");
        if (price == null || price.signum() < 0) throw new IllegalArgumentException("price must be >= 0");
        this.itemId = itemId;
        this.price = price;
    }

    public static ItemSnapshot of(Long itemId, BigDecimal price) {
        return new ItemSnapshot(itemId, price);
    }
}


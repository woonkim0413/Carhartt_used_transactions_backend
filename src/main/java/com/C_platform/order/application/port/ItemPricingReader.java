package com.C_platform.order.application.port;

import java.math.BigDecimal;

public interface ItemPricingReader {

    ItemView getById(Long itemId);

    // 주문 생성에 필요한 최소 정보만
    record ItemView(Long id, Long sellerId, Integer price) {}
}

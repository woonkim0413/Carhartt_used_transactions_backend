package com.C_platform.order.ui.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderCompletionResponse(
        Long orderId,

        // 상품 스냅샷 정보
        Long itemId,
        String itemName,
        BigDecimal itemPrice,

        // 배송지 정보 (OrderAddress에서 필요한 필드만 추출)
        String recipientName,
        String addressDetail,
        String zipCode,

        // 결제 완료 시점의 정보
        LocalDateTime orderDateTime,
        String orderStatus // 'PAID' 상태 확인용
) {}

package com.C_platform.order.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

// 주문 생성 성공 data 부분
@Schema(name="CreateOrderResponse")
public record CreateOrderResponse(
        @JsonProperty("order_id") Long orderId
) {}


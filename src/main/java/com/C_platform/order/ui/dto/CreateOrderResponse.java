package com.C_platform.order.ui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name="CreateOrderResponse")
public record CreateOrderResponse(
        @Schema(example = "202")
        @JsonProperty("order_id") Long orderId
) {
    public static CreateOrderResponse of(Long orderId) {
        return new CreateOrderResponse(orderId);
    }
}



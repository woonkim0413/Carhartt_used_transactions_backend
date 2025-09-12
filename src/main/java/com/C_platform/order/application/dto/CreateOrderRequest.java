package com.C_platform.order.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name="CreateOrderRequest")
public record CreateOrderRequest(
        @JsonProperty("item_id") Long itemId,
        @JsonProperty("address_id") Long addressId,
        @JsonProperty("payment_method") String paymentMethod,
        @JsonProperty("detail_message") String detailMessage
) {}


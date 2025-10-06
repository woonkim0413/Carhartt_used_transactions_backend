package com.C_platform.payment.application.dto.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoCompleteResponse(
        String aid,
        String tid,
        @JsonProperty("partner_order_id") String partnerOrderId,
        @JsonProperty("partner_user_id")  String partnerUserId,
        Amount amount
) {
    public record Amount(
            Integer total,
            @JsonProperty("tax_free") Integer taxFree,
            Integer vat,
            @JsonProperty("point") Integer point,
            @JsonProperty("discount") Integer discount
    ) {}
}
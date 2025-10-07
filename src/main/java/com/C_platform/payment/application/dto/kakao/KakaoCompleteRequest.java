package com.C_platform.payment.application.dto.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record KakaoCompleteRequest(
        String cid,
        String tid,
        @JsonProperty("partner_order_id") String partnerOrderId,
        @JsonProperty("partner_user_id")  String partnerUserId,
        @JsonProperty("pg_token")         String pgToken
) {
    public Map<String, String> toForm() {
        return Map.of(
                "cid", cid,
                "tid", tid,
                "partner_order_id", partnerOrderId,
                "partner_user_id", partnerUserId,
                "pg_token", pgToken
        );
    }
}

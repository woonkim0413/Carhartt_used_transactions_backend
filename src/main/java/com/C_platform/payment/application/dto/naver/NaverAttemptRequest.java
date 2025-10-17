package com.C_platform.payment.application.dto.naver;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NaverAttemptRequest(
        @JsonProperty("merchantPayKey") String merchantPayKey,      // 필수! (주문번호)
        @JsonProperty("productName") String productName,
        @JsonProperty("productCount") int productCount,             // 필수!
        @JsonProperty("totalPayAmount") int totalPayAmount,
        @JsonProperty("taxScopeAmount") int taxScopeAmount,
        @JsonProperty("taxExScopeAmount") int taxExScopeAmount,
        @JsonProperty("returnUrl") String returnUrl
) {
    public static NaverAttemptRequest from(String orderKey, String name, int amount, String returnUrl) {
        return new NaverAttemptRequest(
                orderKey,       // merchantPayKey로 변경
                name,
                1,             // productCount 추가
                amount,
                amount,
                0,
                returnUrl
        );
    }
}



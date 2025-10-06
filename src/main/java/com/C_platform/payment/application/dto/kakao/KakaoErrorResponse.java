package com.C_platform.payment.application.dto.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoErrorResponse(
        @JsonProperty("code")    String code,
        @JsonProperty("msg")     String message
) {}

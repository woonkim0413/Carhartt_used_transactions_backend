package com.C_platform.payment.application.dto.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

// 🚨 여기에 추가: DTO에 정의되지 않은 모든 필드를 무시합니다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoAttemptResponse(
        String tid,
        @JsonProperty("next_redirect_app_url")    String nextRedirectAppUrl,
        @JsonProperty("next_redirect_mobile_url") String nextRedirectMobileUrl,
        @JsonProperty("next_redirect_pc_url")     String nextRedirectPcUrl,
        @JsonProperty("android_app_scheme")       String androidAppScheme,
        @JsonProperty("ios_app_scheme")           String iosAppScheme,
        @JsonProperty("created_at") LocalDateTime createdAt
) {}

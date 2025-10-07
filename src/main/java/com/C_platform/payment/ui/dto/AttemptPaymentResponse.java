package com.C_platform.payment.ui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AttemptPaymentResponse(
        @JsonProperty("next_redirect_app_url")    String nextRedirectAppUrl,
        @JsonProperty("next_redirect_mobile_url") String nextRedirectMobileUrl,
        @JsonProperty("next_redirect_pc_url")     String nextRedirectPcUrl,
        @JsonProperty("android_app_scheme")       String androidAppScheme,
        @JsonProperty("ios_app_scheme")           String iosAppSchemel
) {}

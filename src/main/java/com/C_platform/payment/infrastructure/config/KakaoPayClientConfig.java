package com.C_platform.payment.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class KakaoPayClientConfig {

    @Value("${pay.kakao.base-url:https://kapi.kakao.com}")
    private String baseUrl;

    // ⚠️ 콘솔 가이드에 맞게 헤더 스킴 결정 (예: "SECRET_KEY xxx" 또는 "KakaoAK xxx")
    @Value("${pay.kakao.authorization}")
    private String authorization; // 예: "SECRET_KEY sk_test_xxx" 또는 "KakaoAK admin_key_xxx"

    @Bean
    public WebClient kakaoPayWebClient() {
        System.out.println("=== Kakao Pay Authorization: SECRET_KEY " + authorization);
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, authorization)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .exchangeStrategies(ExchangeStrategies.builder().build())
                .build();
    }
}

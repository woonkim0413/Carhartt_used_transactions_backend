package com.C_platform.payment.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class NaverPayClientConfig {

    @Value("${pay.naver.api-base-url}")
    private String apiBaseUrl;

    @Value("${pay.naver.partner-id}")
    private String partnerId;

    @Value("${pay.naver.client-id}")
    private String clientId;

    @Value("${pay.naver.client-secret}")
    private String clientSecret;

    @Value("${pay.naver.chain-id:}")  // Optional
    private String chainId;

    @Bean("naverPayWebClient")
    public WebClient naverPayWebClient() {
        System.out.println("=== Naver Pay Client ID: " + clientId);
        System.out.println("=== Naver Pay Partner ID: " + partnerId);

        String baseUrl = apiBaseUrl + "/" + partnerId + "/naverpay/payments";

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Naver-Client-Id", clientId)
                .defaultHeader("X-Naver-Client-Secret", clientSecret)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .exchangeStrategies(ExchangeStrategies.builder().build());

        // Chain ID가 있으면 추가 (그룹형 가맹점만)
        if (chainId != null && !chainId.isEmpty()) {
            builder.defaultHeader("X-NaverPay-Chain-Id", chainId);
        }

        return builder.build();
    }
}
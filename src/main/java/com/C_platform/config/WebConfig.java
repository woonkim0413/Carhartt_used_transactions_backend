package com.C_platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Web MVC 설정
 * - HTTP 메시지 변환기 설정 (UTF-8 인코딩)
 * - JSON 요청/응답의 한글 처리
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * HTTP 메시지 변환기 설정
     * JSON 요청/응답을 UTF-8로 인코딩하여 한글 문자 처리
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(jsonHttpMessageConverter());
    }

    /**
     * Jackson 기반 JSON HTTP 메시지 변환기
     * UTF-8 인코딩과 한글 처리를 위해 설정
     */
    @Bean
    public MappingJackson2HttpMessageConverter jsonHttpMessageConverter() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();

        // UTF-8 인코딩 명시적 설정
        converter.setDefaultCharset(StandardCharsets.UTF_8);

        // 지원하는 미디어 타입 설정 (UTF-8 명시)
        List<MediaType> supportedMediaTypes = new ArrayList<>();
        supportedMediaTypes.add(new MediaType("application", "json", StandardCharsets.UTF_8));
        supportedMediaTypes.add(new MediaType("application", "*+json", StandardCharsets.UTF_8));
        converter.setSupportedMediaTypes(supportedMediaTypes);

        return converter;
    }
}

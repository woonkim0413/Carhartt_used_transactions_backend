package com.C_platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Redis 기반 세션 저장소 설정
 *
 * @EnableRedisHttpSession: Spring Session이 HttpSession을 Redis로 저장하도록 설정
 * maxInactiveIntervalInSeconds: 세션 만료 시간 (초 단위, 1800초 = 30분)
 *
 * 이 설정을 활성화하면 Spring Session이 SessionRepositoryFilter를 자동 등록하여
 * HttpServletRequest.getSession()을 가로채서 Tomcat 세션 대신 Redis 기반 세션을 반환합니다.
 * 따라서 기존 코드(SecurityConfig, Filter 등)는 수정할 필요가 없습니다.
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)  // 30분
public class RedisSessionConfig {

    /**
     * RedisTemplate 설정 (선택사항 - 세션 외 Redis 사용 시)
     *
     * Spring Session은 내부적으로 자체 RedisTemplate을 사용하므로
     * 이 빈은 세션 외 용도(캐싱, 메시징 등)로 Redis를 사용할 때만 필요
     *
     * @param connectionFactory Redis 연결 팩토리 (Spring Boot가 자동 생성)
     * @return RedisTemplate 인스턴스
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key: String 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value: JSON 직렬화 (객체를 JSON으로 저장)
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        return template;
    }
}

package com.C_platform.Member_woonkim.utils;

import com.C_platform.Member_woonkim.infrastructure.dto.OAuth2UserInfoDto;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 인증 성공 후, loginCheck까지 사용자 정보를 임시로 저장하는 인메모리 저장소
 * Key: 최초 세션 ID, Value: 인증된 사용자 정보
 */
@Component
public class InMemoryAuthSuccessStore {

    private final Map<String, OAuth2UserInfoDto> store = new ConcurrentHashMap<>();

    public void save(String sessionId, OAuth2UserInfoDto userInfo) {
        // TODO: 만료 시간 추가 (예: 5분)
        store.put(sessionId, userInfo);
    }

    public OAuth2UserInfoDto consume(String sessionId) {
        // 값을 꺼내고 맵에서 바로 삭제 (일회용)
        return store.remove(sessionId);
    }
}

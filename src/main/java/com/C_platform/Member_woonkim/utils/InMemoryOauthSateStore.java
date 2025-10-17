package com.C_platform.Member_woonkim.utils;

import com.C_platform.Member_woonkim.domain.enums.OAuthProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * oauth_state -> origin 을 JVM 메모리에 임시 보관 (TTL=300초 고정).
 * 기능: put(state, origin), exists(state), get(state), remove(state)
 * 만료는 get/exists 시와 백그라운드 청소 스레드(60초 주기)에서 제거.
 */
@Component
public class InMemoryOauthSateStore {

    private static final long TTL_SECONDS = 300; // 5분
    private final Map<String, StateInfo> store = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "oauth-state-cleaner");
        t.setDaemon(true);
        return t;
    });

    public InMemoryOauthSateStore() {
        cleaner.scheduleAtFixedRate(this::evictExpired, 60, 60, TimeUnit.SECONDS);
    }

    /** 저장: state -> (origin, provider, issuedAt, expiresAt) */
    public void put(String state, String origin, OAuthProvider provider) {
        if (isBlank(state)) return;
        long issuedAtMs = System.currentTimeMillis();
        Instant expiresAt = Instant.ofEpochMilli(issuedAtMs).plusSeconds(TTL_SECONDS);
        store.put(state, new StateInfo(origin, provider, issuedAtMs, expiresAt));
    }

    /** 1회성 소비 + 검증을 내부에서 수행, 성공 시 origin만 반환 */
    public String consumeOrigin(String state, OAuthProvider expectedProvider) {
        if (state == null) return null;
        StateInfo info = store.remove(state);   // ★ 1회성 제거
        if (info == null) return null;
        if (Instant.now().isAfter(info.expiresAt)) return null; // 만료
        if (info.provider != expectedProvider) return null;     // 제공자 불일치
        return info.origin; // 성공: origin만 반환
    }

    /** (선택) 조회 전용: 만료면 제거하고 null, 아니면 origin만 반환(소비 X) */
    public String getOrigin(String state) {
        if (state == null) return null;
        StateInfo info = store.get(state);
        if (info == null) return null;
        if (Instant.now().isAfter(info.expiresAt)) {
            store.remove(state);
            return null;
        }
        return info.origin;
    }

    // --- 내부 ---

    private void evictExpired() {
        Instant now = Instant.now();
        store.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt));
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private static final class StateInfo {
        final String origin;
        final OAuthProvider provider;
        final long issuedAtMs;
        final Instant expiresAt;
        StateInfo(String origin, OAuthProvider provider, long issuedAtMs, Instant expiresAt) {
            this.origin = origin == null ? "" : origin;
            this.provider = provider;
            this.issuedAtMs = issuedAtMs;
            this.expiresAt = expiresAt;
        }
    }
}
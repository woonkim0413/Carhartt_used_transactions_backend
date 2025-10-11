package com.C_platform.Member_woonkim.utils;

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

    private static final long TTL_SECONDS = 300; // 🔒 매직넘버: 300초 고정

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "oauth-state-cleaner");
        t.setDaemon(true);
        return t;
    });

    public InMemoryOauthSateStore() {
        // 60초마다 만료 스윕
        cleaner.scheduleAtFixedRate(this::evictExpired, 60, 60, TimeUnit.SECONDS);
    }

    /** 저장(동일 state면 덮어씀) */
    public void put(String oauthState, String origin) {
        if (isBlank(oauthState)) return;
        Instant expiresAt = Instant.now().plusSeconds(TTL_SECONDS);
        store.put(oauthState, new Entry(origin, expiresAt));
    }

    /** 존재 여부(만료면 false 반환하며 내부에서 제거) */
    public boolean exists(String oauthState) {
        if (oauthState == null) return false;
        Entry e = store.get(oauthState);
        if (e == null) return false;
        if (Instant.now().isAfter(e.expiresAt)) {
            store.remove(oauthState);
            return false;
        }
        return true;
    }

    /** 저장된 origin 반환(만료면 null 반환하며 내부에서 제거) */
    public String get(String oauthState) {
        if (oauthState == null) return null;
        Entry e = store.get(oauthState);
        if (e == null) return null;
        if (Instant.now().isAfter(e.expiresAt)) {
            store.remove(oauthState);
            return null;
        }
        return e.origin;
    }

    /** 명시적 삭제(원타임 소비용) */
    public void remove(String oauthState) {
        if (oauthState != null) store.remove(oauthState);
    }

    // ---------- 내부 헬퍼 ----------

    private void evictExpired() {
        Instant now = Instant.now();
        store.entrySet().removeIf(ent -> now.isAfter(ent.getValue().expiresAt));
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private static final class Entry {
        final String origin;
        final Instant expiresAt;
        Entry(String origin, Instant expiresAt) {
            this.origin = origin == null ? "" : origin;
            this.expiresAt = expiresAt;
        }
    }
}
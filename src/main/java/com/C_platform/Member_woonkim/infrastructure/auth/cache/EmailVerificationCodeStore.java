package com.C_platform.Member_woonkim.infrastructure.auth.cache;

import com.C_platform.Member_woonkim.exception.EmailErrorCode;
import com.C_platform.Member_woonkim.exception.EmailException;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import javax.security.auth.login.LoginException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 메모리 기반 이메일 검증 코드 저장소
 * 인증 코드를 임시로 저장하고 만료 시간을 관리합니다.
 * (Redis 없는 로컬 개발 환경용)
 */
@Component
@Slf4j
public class EmailVerificationCodeStore {

    private static final String KEY_PREFIX = "verification:";
    private static final long TTL_SECONDS = 600; // 10분

    // 코드 저장소: {email -> CodeEntry{code, expirationTime}}
    private final ConcurrentHashMap<String, CodeEntry> codeStore = new ConcurrentHashMap<>();

    // 검증 완료 저장소: {email -> expirationTime}
    private final ConcurrentHashMap<String, Long> verifiedStore = new ConcurrentHashMap<>();

    // 만료된 항목 정리를 위한 스케줄러
    private final ScheduledExecutorService scheduler;

    // todo : 학습 필요
    public EmailVerificationCodeStore() {
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "EmailVerificationCodeStore-Cleanup");
            t.setDaemon(true);
            return t;
        });

        // 1분마다 만료된 항목 정리
        scheduler.scheduleAtFixedRate(this::cleanupExpiredEntries, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * 인증 코드를 메모리에 저장 (TTL: 10분)
     * @param email 이메일 주소
     * @param code 인증 코드
     */
    public void saveCode(String email, String code) {
        String key = buildKey(email);
        long expirationTime = System.currentTimeMillis() + (TTL_SECONDS * 1000);
        codeStore.put(key, new CodeEntry(code, expirationTime));
        log.debug("Verification code saved for email: {}", email);
    }

    /**
     * 메모리에서 코드 조회
     * @return 저장된 코드 (없거나 만료된 경우 Optional.empty())
     */
    public Optional<String> getCode(String email) {
        String key = buildKey(email);
        CodeEntry entry = codeStore.get(key);

        if (entry == null) {
            log.debug("Verification code not found for email: {}", email);
            return Optional.empty();
        }

        // 만료 여부 확인
        if (System.currentTimeMillis() > entry.expirationTime) {
            log.debug("Verification code expired for email: {}", email);
            return Optional.empty();
        }

        log.debug("Verification code retrieved for email: {}", email);
        return Optional.of(entry.code);
    }

    /**
     * 검증된 이메일 표시 (signup 시 확인용)
     * @param email 이메일 주소
     */
    public void markAsVerified(String email) {
        String key = buildVerifiedKey(email);
        long expirationTime = System.currentTimeMillis() + (TTL_SECONDS * 1000);
        verifiedStore.put(key, expirationTime);
        log.debug("Email marked as verified: {}", email);
    }

    /**
     * 검증 여부 확인
     * @param email 이메일 주소
     * @return 검증 여부
     */
    public boolean isVerified(String email) {
        String key = buildVerifiedKey(email);
        Long expirationTime = verifiedStore.get(key);

        if (expirationTime == null) {
            throw new EmailException(EmailErrorCode.E006);
        }

        // 만료 여부 확인
        if (System.currentTimeMillis() > expirationTime) {
            verifiedStore.remove(key);
            throw new EmailException(EmailErrorCode.E002);
        }
        log.info("이메일 인증 여부 확인 완료 (이메인 인증 완료 상태)");
        return true;
    }

    /**
     * 검증 상태 제거 (signup 완료 후)
     * @param email 이메일 주소
     */
    public void clearVerification(String email) {
        String key = buildVerifiedKey(email);
        verifiedStore.remove(key);
        log.debug("Verification cleared for email: {}", email);
    }

    /**
     * 만료된 항목 정리
     */
    private void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();

        // 코드 저장소 정리 {email, CodeEntry}
        codeStore.entrySet().removeIf(entry ->
            entry.getValue().expirationTime < now);

        // 검증 저장소 정리 {email, ExpireationTime}
        verifiedStore.entrySet().removeIf(entry ->
            entry.getValue() < now);

        log.debug("Cleanup completed. Code store size: {}, Verified store size: {}",
            codeStore.size(), verifiedStore.size());
    }

    private String buildKey(String email) {
        return KEY_PREFIX + email;
    }

    private String buildVerifiedKey(String email) {
        return KEY_PREFIX + "verified:" + email;
    }

    /**
     * 코드 저장 엔트리
     */
    private static class CodeEntry {
        final String code;
        final long expirationTime;

        CodeEntry(String code, long expirationTime) {
            this.code = code;
            this.expirationTime = expirationTime;
        }
    }
}

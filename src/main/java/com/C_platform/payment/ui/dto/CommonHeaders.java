package com.C_platform.payment.ui.dto;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

public record CommonHeaders(
        String requestId,
        String serverTimezone,
        String serverTimeFormat,
        String idempotencyKey
) {
    public static CommonHeaders from(HttpServletRequest req) {
        return new CommonHeaders(
                headerOr(req, "X-Request-Id", "req-" + UUID.randomUUID()),
                headerOr(req, "X-Server-Timezone", "UTC"),
                headerOr(req, "X-Server-Time-Format", "ISO-8601"),
                headerOr(req, "Idempotency-Key", UUID.randomUUID().toString())
        );
    }

    private static String headerOr(HttpServletRequest r, String k, String def) {
        String v = r.getHeader(k);
        return (v == null || v.isBlank()) ? def : v;
    }
}


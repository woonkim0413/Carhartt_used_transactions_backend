package com.C_platform.order.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

// 공통 응답 wrapper
@Schema(name="ApiResponse")
public record ApiResponse<T>(
        boolean success,
        T data,
        Meta meta
) {
    public static <T> ApiResponse<T> success(T data, String requestId, String traceId) {
        return new ApiResponse<>(true, data, new Meta(OffsetDateTime.now(), requestId, traceId));
    }

    public record Meta(
            @JsonProperty("server_time") OffsetDateTime serverTime,
            @JsonProperty("request_id") String requestId,
            @JsonProperty("trace_id") String traceId
    ) {}
}

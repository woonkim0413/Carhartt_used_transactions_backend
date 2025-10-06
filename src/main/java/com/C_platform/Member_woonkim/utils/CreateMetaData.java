package com.C_platform.Member_woonkim.utils;

import com.C_platform.global.MetaData;

import java.time.LocalDateTime;

public class CreateMetaData {
    public static MetaData createMetaData(LocalDateTime localDateTime) {
        return MetaData.builder()
                .timestamp(localDateTime)
                .build();
    }

    // debugging 추적 값(X-Request-Id) 포함
    public static MetaData createMetaData(LocalDateTime localDateTime, String xRequestId) {
        return MetaData.builder()
                .timestamp(localDateTime)
                .xRequestId(xRequestId)
                .build();
    }
}

package com.C_platform.item.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * 상의
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TopItemEmbeddable  {
    @Column(name = "chest" ,nullable = false)
    private int chest; // 가슴 단면

    @Column(name = "shoulder", nullable = false)
    private int shoulder; // 어깨

    @Column(name = "sleeve", nullable = false)
    private int sleeve; // 소매

    @Column(name = "rise_length", nullable = false)
    @Builder.Default
    private int riseLength = 0; // 밑위

    @Column(name = "thigh", nullable = false)
    @Builder.Default
    private int thigh = 0; // 허벅지

    @Column(name = "hem", nullable = false)
    @Builder.Default
    private int hem = 0; // 밑단
}

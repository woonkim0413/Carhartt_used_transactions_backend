package com.C_platform.item.entity;

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
public class TopItemEmbeddable  {
    @Column(name = "chest" ,nullable = false)
    private int chest; // 가슴 단면

    @Column(name = "shoulder", nullable = false)
    private int shoulder; // 어깨

    @Column(name = "sleeve", nullable = false)
    private int sleeve; // 소매
}

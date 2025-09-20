package com.C_platform.item.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * 하의
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BottomItemEmbeddable  {
    @Column(name = "rise_length", nullable = true)
    private int riseLength; // 밑위

    @Column(name = "thigh", nullable = true)
    private int thigh; // 허벅지

    @Column(name = "hem", nullable = true)
    private int hem; // 밑단
}

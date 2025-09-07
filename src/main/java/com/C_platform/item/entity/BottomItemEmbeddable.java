package com.C_platform.item.entity;

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
    @Column(name = "rise_length", nullable = false)
    private int riseLength; // 밑위

    @Column(name = "thigh", nullable = false)
    private int thigh; // 허벅지

    @Column(name = "hem", nullable = false)
    private int hem; // 밑단
}

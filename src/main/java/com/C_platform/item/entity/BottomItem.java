package com.C_platform.item.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

/**
 * 하의
 */
@Embeddable
@Getter
@Setter
public class BottomItem {
    @Column(name = "rise_length", nullable = false,columnDefinition = "INT(5)")
    private Integer riseLength; // 밑위

    @Column(name = "thigh", nullable = false,columnDefinition = "INT(5)")
    private Integer thigh; // 허벅지

    @Column(name = "hem", nullable = false,columnDefinition = "INT(5)")
    private Integer hem; // 밑단
}

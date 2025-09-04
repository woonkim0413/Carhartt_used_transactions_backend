package com.C_platform.item.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("TOP") // 실제 d_type 값에 맞게 조정
public class TopItem extends Item {
    // 상의 전용 도메인 로직이 있으면 추가
}
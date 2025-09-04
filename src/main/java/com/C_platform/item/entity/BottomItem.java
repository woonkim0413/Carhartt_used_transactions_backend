package com.C_platform.item.entity;


import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("BOTTOM") // 실제 d_type 값에 맞게 조정
public class BottomItem extends Item {
    // 하의 전용 도메인 로직이 있으면 추가
}
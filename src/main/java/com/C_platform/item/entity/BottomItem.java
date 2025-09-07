package com.C_platform.item.entity;


import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@DiscriminatorValue("BOTTOM") // DTYPE 값이 'P'일 경우 Pants 엔티티로 매핑
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BottomItem extends Item {
    @Embedded
    private BottomItemEmbeddable bottomInfo;
}
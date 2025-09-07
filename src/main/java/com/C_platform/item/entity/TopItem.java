package com.C_platform.item.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@DiscriminatorValue("TOP") // DTYPE 값이 'T'일 경우 Top 엔티티로 매핑
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TopItem extends Item {

    @Embedded
    private TopItemEmbeddable topinfo;
}
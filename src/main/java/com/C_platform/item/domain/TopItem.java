package com.C_platform.item.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@DiscriminatorValue("TOP")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder // @Builder 대신 @SuperBuilder 사용
public class TopItem extends Item {

    @Embedded
    private TopItemEmbeddable topinfo;
}
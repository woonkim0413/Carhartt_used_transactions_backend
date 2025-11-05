package com.C_platform.item.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@DiscriminatorValue("Outer")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class OuterItem extends Item {

    @Embedded
    private OuterItemEmbeddable outerinfo;

    public void updateSize(OuterItemEmbeddable outerinfo) {
        this.outerinfo = outerinfo;
    }
}

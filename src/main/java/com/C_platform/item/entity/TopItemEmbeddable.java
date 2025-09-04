package com.C_platform.item.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

/**
 * 상의
 */
@Embeddable
@Getter
@Setter
public class TopItemEmbeddable  {
    @Column(name = "chest" ,nullable = false , columnDefinition = "INT(5)")
    private Integer chest; // 가슴 단면

    @Column(name = "shoulder", nullable = false , columnDefinition = "INT(5)")
    private Integer shoulder; // 어깨

    @Column(name = "sleeve", nullable = false ,columnDefinition = "INT(5)")
    private Integer sleeve; // 소매
}

package com.C_platform.item.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "images")
public class Images {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id; // 이미지 ID

    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl; // 이미지 URL


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    @JsonBackReference
    private Item item;

    @Column(name = "represent_url", nullable = false , length = 255 )
    private String representUrl; // 대표 이미지
}

package com.C_platform.item.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "images")
public class Images {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="image_id")
    private Long id;

    @Column(name = "image_url",nullable = false, length = 255)
    private String imageUrl;

    @Column(name = "represent_url", nullable = false, length = 255)
    private String representUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;
}
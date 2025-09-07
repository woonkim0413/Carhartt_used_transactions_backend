package com.C_platform.item.entity;


import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "item")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id;

    @Column(name = "item_name", nullable = false, length = 255)
    private String name;

    @Column(name = "item_price", nullable = false)
    private Integer price;

    @Column(name = "og_price", nullable = false)
    private Integer ogPrice;

    @Column(name = "item_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ItemStatus status;

    @Column(name = "signed_date", nullable = false)
    private LocalDateTime signedDate;

    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate;

    @ManyToMany
    @JoinTable(
            name = "category_join_Item", // 중간 테이블 이름
            joinColumns = @JoinColumn(name = "item_id"), // 현재 엔티티(Item)를 참조하는 FK
            inverseJoinColumns = @JoinColumn(name = "category_id") // 반대쪽 엔티티(Category)를 참조하는 FK
    )
    private List<Category> categories = new ArrayList<>();


}
package com.C_platform.item.domain;


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

    @OneToMany(mappedBy = "item")
    private List<CategoryItem> categories = new ArrayList<>();


}
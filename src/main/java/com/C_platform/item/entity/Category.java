package com.C_platform.item.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "category")
@Setter
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int categoryId; // 카테고리 ID

    @Column(name = "parent_category")
    private String parentCategory; // 상위 카테고리

    @Column(name = "category_name", nullable = false, length = 255)
    private String categoryName;

}

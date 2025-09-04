package com.C_platform.item.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "category")
@Getter
@Setter
public class Category {

    @Id
    @Column(name = "category_id")
    private Integer id;

    // VARCHAR 부모키. 서비스 단에서 정수 변환 필요 시 처리
    @Column(name = "parent_category")
    private String parentCategory;

    @Column(name = "category_name", nullable = false, length = 255)
    private String name;

    // 선택: 역방향. 필요 없으면 제거 가능.
    @ManyToMany(mappedBy = "categories")
    private Set<Item> items = new LinkedHashSet<>();

    // equals/hashCode는 식별자 기반
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }

    public Integer parentIdOrZero() {
        if (parentCategory == null || parentCategory.isBlank()) return 0;
        try { return Integer.parseInt(parentCategory); } catch (NumberFormatException e) { return 0; }
    }



}

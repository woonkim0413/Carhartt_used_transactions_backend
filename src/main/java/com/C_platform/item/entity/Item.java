package com.C_platform.item.entity;


import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "item")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "d_type")
@Getter
@Setter // 불변 객체... 찾아보기
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id;

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    // ERD 오타 유지: itme_price
    @Column(name = "itme_price", nullable = false)
    private Integer itemPrice;

    // 사이즈 컬럼: 단일테이블이므로 모두 nullable=true
    @Column(name = "total_length")
    private Integer totalLength;

    @Column(name = "chest")
    private Integer chest;

    @Column(name = "shoulder")
    private Integer shoulder;

    @Column(name = "sleeve")
    private Integer sleeve;

    @Column(name = "rise_length")
    private Integer riseLength;

    @Column(name = "thigh")
    private Integer thigh;

    @Column(name = "hem")
    private Integer hem;

    @Column(name = "signed_date", nullable = false)
    private LocalDateTime signedDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    // 카테고리 연결: 조인테이블 표기 ERD 준수 (I 대문자)
    @ManyToMany
    @JoinTable(
            name = "category_join_Item",
            joinColumns = @JoinColumn(name = "item_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new LinkedHashSet<>();

    // 이미지 양방향
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Images> images = new ArrayList<>();

    // equals/hashCode는 식별자 기반
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item that)) return false;
        return id != null && id.equals(that.id);
    }
    @Override
    public int hashCode() { return Objects.hashCode(id); }

    // getters/setters ...
}
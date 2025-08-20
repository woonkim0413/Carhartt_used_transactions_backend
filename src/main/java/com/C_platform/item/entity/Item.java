package com.C_platform.item.entity;


import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="item")
@Getter
@Setter
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY )
    private int id; // 상품 ID

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName; // 상품 이름

    @Column(name="item_price", nullable = false,columnDefinition = "INT(20)")
    private Integer itemPrice; // 상품 가격

    @Column(name="d_type" , nullable = false , length = 255)
    private String dType; // 상품 타입

    @Column(name = "total_length", nullable = false ,columnDefinition = "INT(5)")
    private Integer totalLength; // 총 장

    @Embedded
    private TopItem topItem; // 상의

    @Embedded
    private BottomItem bottomItem; // 하의

    @Column(name = "item_status", nullable = false)
    private ItemStatus itemStatus; // 상품 상태

    @Column(name = "og_price" , columnDefinition = "INT(20)")
    private Integer ogPrice; // 원가

    @Column(name = "signed_date", nullable = false)
    private String signedDate; // 등록일

    @Column(name = "updated_date")
    private String updatedDate; // 수정일


    // 이미지는 Item이 inverse side (mappedBy) - 컬렉션은 LAZY로 유지
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Images> imagesList = new ArrayList<>(); // 이미지 리스트

}

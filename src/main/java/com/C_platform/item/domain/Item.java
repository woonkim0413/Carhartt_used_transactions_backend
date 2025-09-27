package com.C_platform.item.domain;



import com.C_platform.Member.domain.Member.Member;
import com.C_platform.order.domain.Order;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder // @Builder 대신 @SuperBuilder 사용
@Entity
@Table(name = "item")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "item_name", nullable = false, length = 255)
    private String name;

    @Column(name = "item_price", nullable = false)
    private Integer price;

//    @Column(name = "og_price", nullable = true)
//    private Integer ogPrice;

    @Column(name = "item_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ItemStatus status;

    @Column(name="total_length", nullable = false)
    private Integer totalLength;

    @Column(name = "signed_date", nullable = false)
    private LocalDateTime signedDate;

    @Column(name = "update_date", nullable = true)
    private LocalDateTime updateDate;

    @Column(name = "description", nullable = true)
    private String description;

    @Column(name = "direct_trade", nullable = true)
    private Boolean directTrade;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CategoryItem> categories = new ArrayList<>();

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Images> images = new ArrayList<>();




    /**
     * 주문이 들어오면 상품 상태를 SOLD_OUT으로 변경
     * @param order
     */
    public void placeOrder(Order order) {
        if(order != null) {
            this.status = ItemStatus.SOLD_OUT;
        }
    }

    /**
     * 상품의 상세정보를 업데이트 하는 메서드
     */
    public void updateDetails(String name, Integer price, String description, Boolean directTrade) {
        this.name = name;
        this.price = price;
        this.description = description;
        this.directTrade = directTrade;
        this.updateDate = LocalDateTime.now();
    }

    /**
     * 상품의 카테고리를 변경하는 메서드
     */
    public void changeCategory(Category category) {
        this.categories.clear();
        CategoryItem categoryItem = CategoryItem.builder()
                .category(category)
                .item(this)
                .build();
        this.categories.add(categoryItem);
    }

    public boolean isTrade() {
        return Boolean.TRUE.equals(directTrade);
    }
}
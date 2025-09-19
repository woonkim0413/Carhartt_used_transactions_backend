package com.C_platform.Member.domain.Member;

import jakarta.persistence.*;
import lombok.Setter;

@Entity
@Setter
public class Wish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_id")
    private Long id;


    // Item 엔티티가 있으면 @ManyToOne으로 교체 가능
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    // ---------- 연관관계 객체들 -----------------------
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
}

package com.C_platform.Member_woonkim.domain.member_entity;

import jakarta.persistence.*;
import lombok.Setter;

@Entity
@Setter
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long id;

    // 예: "집", "회사", "부모님댁" 등
    @Column(name = "address_name")
    private String addressName;

    // 우편번호
    @Column(name = "zip")
    private String zip;

    // 도로명 주소
    @Column(name = "road_address")
    private String roadAddress;

    // 상세 주소
    @Column(name = "detail_address")
    private String detailAddress;

    // ---------- 연관관계 객체들 -----------------------

    // 🔗 주소 소유자 (N:1)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;
}

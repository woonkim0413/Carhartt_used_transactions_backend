package com.C_platform.Member.domain.Member;

import com.C_platform.Member.domain.Address.Address;
import com.C_platform.Member.domain.Oauth.LoginType;
import com.C_platform.Member.domain.Oauth.OAuthProvider;
import com.C_platform.order.domain.Order;
import jakarta.persistence.*;
import lombok.Getter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "member")
public class Member {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "member_name")
    private String name;

    @Column(name = "email") // local일 때 사용
    private String email;

    @Column(name = "login_password") // local일 때 사용
    private String loginPassword;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider")
    private OAuthProvider oauthProvider;

    @Column(name = "oauth_id")
    private String oauthId;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_type")
    private LoginType loginType;

    @JoinColumn(name = "address_id")
    private int defaultAddressId;

    @Column(name = "image_url")
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "count_bank")
    private BankName accountBank;

    @Column(name = "count_number") // = account number
    private String accountNumber;

    // ---------- 연관관계 객체들 -----------------------
    // Address와 연관관계로 묶인 field
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();

    // 내가 '구매자'인 주문 목록
    @OneToMany(mappedBy = "buyer")
    private List<Order> purchaseOrders = new ArrayList<>();

    // 내가 '판매자'인 주문 목록
    @OneToMany(mappedBy = "seller")
    private List<Order> salesOrders = new ArrayList<>();

    // Wish와 연관관계로 묶인 field
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Wish> wishes = new ArrayList<>();
}

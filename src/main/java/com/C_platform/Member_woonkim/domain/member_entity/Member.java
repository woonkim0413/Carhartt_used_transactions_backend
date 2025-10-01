package com.C_platform.Member_woonkim.domain.member_entity;

import com.C_platform.Member_woonkim.domain.member_enum.BankName;
import com.C_platform.Member_woonkim.domain.member_enum.LocalProvider;
import com.C_platform.Member_woonkim.domain.member_enum.LoginType;
import com.C_platform.Member_woonkim.domain.member_enum.OAuthProvider;
import com.C_platform.order.domain.Order;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(
    name = "member",
    uniqueConstraints = {
        // oauth login인 경우에 사용하는 회원가입 식별 복합키
        @UniqueConstraint(
            name = "uk_member_oauth",
            columnNames = {"oauth_provider", "oauth_id"}
        ),
        // local login인 경우에 사용하는 회원가입 식별 복합키
        @UniqueConstraint(
            name = "uk_member_local_provider_email",
            columnNames = {"local_provider", "email"}
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 필수
public class Member {
    // nullable 명시하지 않으면 default는 허용임
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "member_name", nullable = false)
    private String name;

    // local일 때 사용
    @Column(name = "email", nullable = false)
    private String email;

    // local일 때 사용
    @Column(name = "login_password")
    private String loginPassword;

    // 이미 회원가입 한 멤버인지 확인할 때 사용 (+ oauthId)
    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider")
    private OAuthProvider oauthProvider;

    @Enumerated(EnumType.STRING)
    @Column(name = "local_provider")
    private LocalProvider localProvider;

    // 이미 회원가입 한 멤버인지 확인할 때 사용 (+ Provider)
    @Column(name = "oauth_id")
    private String oauthId;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_type", nullable = false)
    private LoginType loginType;

    @JoinColumn(name = "address_id")
    private int defaultAddressId;

    @Enumerated(EnumType.STRING)
    @Column(name = "count_bank")
    private BankName accountBank;

    @Column(name = "count_number") // = account number
    private String accountNumber;

    @Column(name = "image_url")
    private String profileImageUrl;

    /** OAuth 전용 생성자 */
    public Member(OAuthProvider oauthProvider, String oauthId, String name, String email) {
        this.loginType = LoginType.OAUTH;
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
        this.name = name;
        this.email = email;
    }

    /** Local 전용 생성자 */
    public Member(LocalProvider localProvider, String email, String encodedPassword, String name) {
        this.loginType = LoginType.LOCAL;
        this.localProvider = localProvider;
        this.email = email;
        this.loginPassword = encodedPassword;
        this.name = name;
    }

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

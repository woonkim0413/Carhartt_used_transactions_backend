package com.C_platform.Member.entity;

import com.C_platform.order.Address;
import com.C_platform.order.domain.Order;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

//@Entity
@Getter @Setter
public class Member {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private int Id;

    // Order에서 연관관계로 묶인 field name
    @OneToMany(mappedBy = "member")
    private List<Order> orders = new ArrayList<>();

    @Embedded
    private Address address;

    private String Name;

    private String login_id;

    private String password;

    private String oauth_provider;

    private String oauth_id;

   // private loginType login_type;
}

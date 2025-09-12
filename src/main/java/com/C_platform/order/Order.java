package com.C_platform.order;

import com.C_platform.Member.domain.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter @Setter
public class Order {

    @Id @GeneratedValue
    @Column(name = "order_id")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // db 저장을 고려하면 Instanct type이 적절함
    private Instant order_date;
}

package com.C_platform.order;

import com.C_platform.order.domain.Order;

import com.C_platform.Member_woonkim.domain.entitys.Address;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

//@Entity
@Getter @Setter
public class Delivery {
    @Id
    @GeneratedValue
    @Column(name = "delivery_id")
    private int id;

    @OneToOne(mappedBy = "delivery", fetch = FetchType.LAZY)
    private Order order;

    /** ✅ 변경: OrderAddress → Address 참조 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;
}

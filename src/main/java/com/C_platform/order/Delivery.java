package com.C_platform.order;

import com.C_platform.order.domain.Order;

import com.C_platform.order.domain.OrderAddress;
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

    /** ✅ 변경: Address → OrderAddress */
    @Embedded
    private OrderAddress addressSnapshot;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;
}

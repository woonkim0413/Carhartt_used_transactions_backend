package com.C_platform.order.domain;

import com.C_platform.order.Address;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_address")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_address_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String city;

    @Column(nullable = false, length = 100)
    private String street;

    @Column(nullable = false, length = 20)
    private String homeNumber;

    // 선택: 수령인 이름, 연락처, 배송 요청사항 등도 필요하면 추가
    // private String receiverName;
    // private String phone;
    // private String requestMessage;

    // 생성자 (팩토리 메서드에서 호출)
    private OrderAddress(String city, String street, String homeNumber) {
        this.city = city;
        this.street = street;
        this.homeNumber = homeNumber;
    }

    /** Member.Address 값 객체를 주문 시점에 스냅샷으로 변환 */
    public static OrderAddress snapshotOf(Address address) {
        return new OrderAddress(
                address.getCity(),
                address.getStreet(),
                address.getHome_number()
        );
    }
}


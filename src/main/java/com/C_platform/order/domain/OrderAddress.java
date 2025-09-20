package com.C_platform.order.domain;

import com.C_platform.order.Address;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderAddress {

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

    /** 주문 생성 시, 멤버의 주소 값을 '복사'해서 스냅샷으로 만듭니다. */
    public static OrderAddress snapshotOf(Address address) {
        // Address의 실제 게터명에 맞춰 아래 한 줄을 선택하세요.
        // return new OrderAddress(address.getCity(), address.getStreet(), address.getHome_number()); // 필드명이 home_number인 경우
        return new OrderAddress(address.getCity(), address.getStreet(), address.getHomeNumber());     // 필드명이 homeNumber인 경우
    }

    /** 직접 값으로 만드는 팩토리 */
    public static OrderAddress of(String city, String street, String homeNumber) {
        return new OrderAddress(city, street, homeNumber);
    }
}


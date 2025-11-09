package com.C_platform.order.domain;

import com.C_platform.Member_woonkim.domain.entitys.Address;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.*;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderAddress {

    @Column(name = "recipient_name", length = 50)  // ← 추가
    private String recipientName;

    @Column(name = "zip_code", length = 20)  // ← 추가
    private String zipCode;

    @Column(name = "road_address", length = 200)  // ← 변경
    private String roadAddress;

    @Column(name = "detail_address", length = 200)  // ← 변경
    private String detailAddress;

    // 생성자
    private OrderAddress(
            String recipientName,
            String zipCode,
            String roadAddress,
            String detailAddress
    ) {
        this.recipientName = recipientName;
        this.zipCode = zipCode;
        this.roadAddress = roadAddress;
        this.detailAddress = detailAddress;
    }

    /** 주문 생성 시 Address 스냅샷 */
    public static OrderAddress snapshotOf(Address address) {
        return new OrderAddress(
                address.getAddressName(),  // recipientName으로 사용
                address.getZip(),
                address.getRoadAddress(),
                address.getDetailAddress()
        );
    }

    /** 직접 값으로 만드는 팩토리 */
    public static OrderAddress of(
            String recipientName,
            String zipCode,
            String roadAddress,
            String detailAddress
    ) {
        return new OrderAddress(recipientName, zipCode, roadAddress, detailAddress);
    }
}


package com.C_platform.order.domain;

import com.C_platform.Member_woonkim.domain.entitys.Address;
import com.C_platform.Member_woonkim.domain.entitys.Member;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

//SQL 예약어 Order by와 충돌 위험 있어서 이름 orders로 수정
@Table(name = "orders")
@Entity
@Builder(access = AccessLevel.PRIVATE)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id", nullable = false)
    private Long id;

    @Column(name = "order_date_time", nullable = false)
    private LocalDateTime orderDateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", length = 20, nullable = false)
    private OrderStatus orderStatus;

    // after
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    @Embedded
    private ItemSnapshot itemSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = true) //buyer, seller 정보 추가되면 false로 수정
    private Member buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = true) //buyer, seller 정보 추가되면 false로 수정
    private Member seller;

    @Column(name = "detail_message", nullable = true)
    private String detailMessage;

    //멱등성용 키
    @Column(name = "request_id", unique = true, length = 128)
    private String requestId;

    // setter (또는 빌더에 포함)
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    // 공개 팩토리 메서드
    public static Order createOrder(Member buyer, Member seller, Address address, String detailMessage, ItemSnapshot itemSnapshot) {
        return Order.builder()
                .buyer(buyer)
                .seller(seller)
                .address(address)
                .detailMessage(detailMessage)
                .itemSnapshot(itemSnapshot)
                .orderDateTime(LocalDateTime.now())
                .orderStatus(OrderStatus.READY)
                .build();
    }

    //핵심 상태 전이 메서드 주문 생성 => 주문 완료
    //예외 처리 확인 필요
    //낙관적 락 도입 검토
    public void makePaid() {
        if (this.orderStatus != OrderStatus.READY) {
            throw new IllegalStateException("주문 상태가 READY가 아니면 결제 완료로 바꿀 수 없습니다.");
        }
        this.orderStatus = OrderStatus.PAID;
    }
}

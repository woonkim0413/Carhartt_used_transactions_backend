package com.C_platform.order.domain;

import com.C_platform.Member.domain.Member.Member;
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

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id", nullable = false)
    private Long id;

    @Column(name = "order_date_time", nullable = false)
    private LocalDateTime orderDateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", length = 20, nullable = false)
    private  OrderStatus orderStatus;

    @Embedded
    private OrderAddress shipping; // 배송지 스냅샷(값 타입)

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

    // 공개 팩토리 메서드
    public static Order createOrder(Member buyer, Member seller, OrderAddress shipping, String detailMessage, ItemSnapshot itemSnapshot) {
        return Order.builder()
                .buyer(buyer)
                .seller(seller)
                .shipping(shipping)
                .detailMessage(detailMessage)
                .itemSnapshot(itemSnapshot)
                .orderDateTime(LocalDateTime.now())
                .orderStatus(OrderStatus.READY)
                .build();
    }

    //임시: buyer/seller 없이 생성 가능한 Draft 팩토리
    public static Order createDraft(OrderAddress shipping, String detailMessage, ItemSnapshot snapshot) {
        if (shipping == null || snapshot == null) throw new IllegalArgumentException("shipping/snapshot required");
        Order o = new Order();
        o.shipping = shipping;
        o.itemSnapshot = snapshot;
        o.orderStatus = OrderStatus.READY;     // 혹은 DRAFT 상태가 있으면 그걸로
        o.orderDateTime = LocalDateTime.now();
        // detailMessage 필드가 있다면 세팅
        return o;
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

package com.C_platform.order.domain;

import java.util.Optional;

//포트(비즈니스 로직이 외부와 소통하는 계약서)
public interface OrderRepository {

    Order save(Order order);
    Optional<Order> findById(Long id); //결제 완료된 주문 하나만 조회

}

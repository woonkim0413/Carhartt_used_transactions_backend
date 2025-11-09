package com.C_platform.order.infrastructure;

import com.C_platform.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

interface OrderJpaRepository extends JpaRepository<Order, Long> {

    // 멱등성 체크용
    boolean existsByRequestId(String requestId);

    // 중복 요청 시 기존 주문 반환용 (선택)
    Optional<Order> findByRequestId(String requestId);

}


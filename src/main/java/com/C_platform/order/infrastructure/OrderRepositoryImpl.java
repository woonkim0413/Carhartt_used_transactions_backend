package com.C_platform.order.infrastructure;

import com.C_platform.order.domain.Order;
import com.C_platform.order.domain.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

//어댑터(포트의 구현체)
@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository jpa;

    @Override
    public Order save(Order order) {
        return jpa.save(order);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return jpa.findById(id);
    }

    @Override
    public boolean existsByRequestId(String requestId) {
        return jpa.existsByRequestId(requestId);
    }
}


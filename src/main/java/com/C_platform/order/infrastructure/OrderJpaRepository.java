package com.C_platform.order.infrastructure;

import com.C_platform.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrderJpaRepository extends JpaRepository<Order, Long> {

}


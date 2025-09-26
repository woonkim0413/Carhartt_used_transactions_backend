package com.C_platform.payment.infrastructure;

import com.C_platform.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrder_Id(Long orderId);
}

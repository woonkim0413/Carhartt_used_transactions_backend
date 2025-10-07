package com.C_platform.payment.infrastructure;

import com.C_platform.payment.domain.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
   //단수 조회
    List<Payment> findByOrderId(Long orderId);

    //승인 처리 시 동시성 제어를 위한 for update 락 걸린 버전
    // 승인 처리 시 동시성 제어를 위해 "for update" 락 걸린 버전
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.order.id = :orderId")
    Optional<Payment> findByOrderIdForUpdate(@Param("orderId") Long orderId);
}

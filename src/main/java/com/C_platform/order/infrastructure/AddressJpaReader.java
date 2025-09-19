package com.C_platform.order.infrastructure;

import com.C_platform.order.application.port.AddressReader;
import com.C_platform.order.domain.OrderAddress;
import org.springframework.stereotype.Component;

@Component
public class AddressJpaReader implements AddressReader {

    // private final MemberAddressRepository repo;  // 나중에 붙이기

    @Override
    public OrderAddress snapshotOf(Long buyerId, Long addressId) {
        // TODO: repo.findByMemberIdAndId(...) 로 조회 후 스냅샷 생성
        // 임시 스텁
        return OrderAddress.of("서울", "강남구 역삼동 824-17", "6층");
    }
}

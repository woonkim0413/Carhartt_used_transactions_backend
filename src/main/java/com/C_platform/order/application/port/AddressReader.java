package com.C_platform.order.application.port;

import com.C_platform.order.domain.OrderAddress;

public interface AddressReader {

    // buyerId의 주소 소유권 검증까지 여기서 하거나, 못하면 Service에서 별도 검증
    OrderAddress snapshotOf(Long buyerId, Long addressId);

}

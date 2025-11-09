package com.C_platform.order.application.port;

import com.C_platform.Member_woonkim.domain.entitys.Address;

public interface AddressReader {

    // Address를 직접 반환
    Address getAddressOrThrow(Long addressId);

}

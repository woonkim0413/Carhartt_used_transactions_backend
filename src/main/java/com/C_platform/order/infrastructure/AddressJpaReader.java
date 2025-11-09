package com.C_platform.order.infrastructure;

import com.C_platform.Member_woonkim.domain.entitys.Address;
import com.C_platform.Member_woonkim.infrastructure.db.AddressRepository;
import com.C_platform.order.application.port.AddressReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AddressJpaReader implements AddressReader {

    private final AddressRepository addressRepository;

    /**
     * ✅ Address 엔티티 조회 + buyer 소유 검증 포함 버전
     */
    @Override
    public Address getAddressOrThrow(Long addressId) {
        Address address = addressRepository.findAddressByAddressId(addressId);
        if (address == null) {
            throw new IllegalArgumentException("주소를 찾을 수 없습니다. id=" + addressId);
        }
        return address;
    }
}

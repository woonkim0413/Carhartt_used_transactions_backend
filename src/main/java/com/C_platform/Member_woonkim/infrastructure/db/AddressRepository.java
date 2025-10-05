package com.C_platform.Member_woonkim.infrastructure.db;

import com.C_platform.Member_woonkim.domain.entitys.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findAllByMember_MemberId(Long memberId);

    Address findAddressByAddressId(Long addressId);
}

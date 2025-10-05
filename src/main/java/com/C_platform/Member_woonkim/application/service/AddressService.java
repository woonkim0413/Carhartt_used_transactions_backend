package com.C_platform.Member_woonkim.application.service;

import com.C_platform.Member_woonkim.domain.entitys.Address;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {
    // TODO : 에러 만들기
    public void addressNumberCheck_MaxFive(List<Address> addressList) {
        final int MAX = 5;
        if (addressList != null && addressList.size() >= MAX) {
            throw new RuntimeException("주소는 최대 " + MAX + "개까지만 등록 가능합니다. (현재: " + addressList.size() + ")");
        }
    }
}

package com.C_platform.Member_woonkim.domain.service;

import com.C_platform.Member_woonkim.domain.entitys.Address;
import com.C_platform.Member_woonkim.exception.AddressErrorCode;
import com.C_platform.Member_woonkim.exception.AddressException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {
    // TODO : 에러 만들기
    public void addressNumberCheck_MaxFive(List<Address> addressList) {
        final int MAX = 5;
        if (addressList != null && addressList.size() >= MAX) {
            // todo : 현재 구조에선 에러 생성 시점에 에러 메세지를 생성하는 것이 불가능함 가능하게 구조 만들기
            // throw new RuntimeException("주소는 최대 " + MAX + "개까지만 등록 가능합니다. (현재: " + addressList.size() + ")");
            throw new AddressException(AddressErrorCode.C001, "주소는 최대" + MAX + "개까지만 등록 가능합니다. (현재: " + addressList.size() + ")");
        }
    }
}

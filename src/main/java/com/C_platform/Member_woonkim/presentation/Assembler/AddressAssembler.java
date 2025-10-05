package com.C_platform.Member_woonkim.presentation.Assembler;

import com.C_platform.Member_woonkim.domain.entitys.Address;
import com.C_platform.Member_woonkim.presentation.dto.response.AddressItemDto;
import com.C_platform.Member_woonkim.presentation.dto.response.DeleteAddressResponseDto;
import com.C_platform.Member_woonkim.presentation.dto.response.GetAddressListResponseDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

@Component
public class AddressAssembler {

    public GetAddressListResponseDto createGetAddressListResponseDto (
            Long member_id,
            List<Address> addressList
    ) {
        List<AddressItemDto> addressItemDtoList = addressToDtoList(addressList);

        return GetAddressListResponseDto.builder()
                .memberId(member_id)
                .addressNumber(Long.valueOf(addressItemDtoList.size()))
                .addressItemDtoList(addressItemDtoList)
                .build();
    }

    // createGetAddressListResponseDto에서 주소 목록을 생성할 때 사용
    private List<AddressItemDto> addressToDtoList (List<Address> addressList) {
        if (addressList == null || addressList.isEmpty())
            return emptyList();
        return addressList.stream() // Stream<Address>로 전환
                .map(this::addressToDto) // Address에 this::addressToAddressItemDto를 적용, Stream<addressToAddressItemDto> 획득
                .toList();
    }

    // addressToDtoList에서 map lambda로 사용
    private AddressItemDto addressToDto(Address address) {
        return AddressItemDto.builder()
                .addressId(address.getAddressId())
                .addressName(address.getAddressName())
                .zipCode(address.getZip())
                .roadAddress(address.getRoadAddress())
                .detailAddress(address.getDetailAddress())
                .build();
    }

    public DeleteAddressResponseDto createDeleteAddressResponseDto () {
        return DeleteAddressResponseDto.builder()
                .deleted(true)
                .build();
    }
}

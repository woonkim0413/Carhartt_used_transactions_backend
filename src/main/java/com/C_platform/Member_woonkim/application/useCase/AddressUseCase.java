package com.C_platform.Member_woonkim.application.useCase;

import com.C_platform.Member_woonkim.domain.service.AddressService;
import com.C_platform.Member_woonkim.domain.entitys.Address;
import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.infrastructure.db.AddressRepository;
import com.C_platform.Member_woonkim.infrastructure.db.MemberRepository;
import com.C_platform.Member_woonkim.presentation.dto.address.request.AddAddressRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressUseCase {

    private final AddressRepository addressRepository;
    private final MemberRepository memberRepository;
    private final AddressService addressService;

    @Transactional
    // TODO : 해당 범위로 transactional 거는 것이 맞는지 + EM은 언제 써야 좋은지 찾아보기
    public Address addAddress(Long member_id, AddAddressRequestDto dto) {

        // todo : 에러 만들기
        Member member = memberRepository.findById(member_id)
                .orElseThrow(() -> new RuntimeException());

        // member에 속한 주소 목록 반환
        List<Address> addressList = getAddressList(member.getMemberId());

        // TODO : 에러 만들기
        // 주소가 5개를 초과한 상태면 에러 터짐
        addressService.addressNumberCheck_MaxFive(addressList);

        // 주소 build
        Address address = Address.builder()
                .addressName(dto.addressName())
                .zip(dto.zipCode())
                .roadAddress(dto.roadAddress())
                .detailAddress(dto.detailAddress())
                .member(member)
                .build();

        addressRepository.save(address);

        // TODO : db에 접근하는 건데 Repo말고 Entity method로 값 변경하는 것이 맞는지 고민
        // memberDefaultAddressId 변경
        member.changeDefaultAddressId(address.getAddressId());

        return address;
    }

    @Transactional
    public List<Address> getAddressList(Long memberId) {
        return getAddressListByDB(memberId);
    }

    @Transactional
    public void deleteAddress(Long memberId, Long addressId) {

        Address address = addressRepository.findAddressByAddressId(addressId);

        // TODO : 에러 만들기
        if (!(address.getMember().getMemberId().equals(memberId))) {
            throw new RuntimeException();
        }

        addressRepository.delete(address);
    }

    private List<Address> getAddressListByDB(Long member_id) {
        return addressRepository.findAllByMember_MemberId(member_id);
    }
}

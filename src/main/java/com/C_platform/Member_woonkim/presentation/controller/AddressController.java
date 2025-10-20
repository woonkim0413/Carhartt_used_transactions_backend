package com.C_platform.Member_woonkim.presentation.controller;


import com.C_platform.Member_woonkim.application.useCase.AddressUseCase;
import com.C_platform.Member_woonkim.domain.entitys.CustomOAuth2User;
import com.C_platform.Member_woonkim.domain.entitys.Address;
import com.C_platform.Member_woonkim.presentation.dtoAssembler.AddressAssembler;
import com.C_platform.Member_woonkim.presentation.dto.address.request.AddAddressRequestDto;
import com.C_platform.Member_woonkim.presentation.dto.address.response.AddAddressResponseDto;
import com.C_platform.Member_woonkim.presentation.dto.address.response.DeleteAddressResponseDto;
import com.C_platform.Member_woonkim.presentation.dto.address.response.GetAddressListResponseDto;
import com.C_platform.Member_woonkim.utils.CreateMetaData;
import com.C_platform.Member_woonkim.utils.LogPaint;
import com.C_platform.global.ApiResponse;
import com.C_platform.global.MetaData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class AddressController {

    private final AddressUseCase addressUseCase;
    private final AddressAssembler addressAssembler;

    @PutMapping("orders/address")
    @Operation(summary = "주소 추가", description = """
    현재 로그인 사용자가 가진 주소 목록에 주소를 추가합니다 <br/>
    주소지 이름은 중복해서 넣을 수 없습니다 <br/>
    사용자는 최대 5개의 주소지를 갖습니다 <br/>
    마지막에 넣은 주소지가 default 주소지로 설정됩니다 (추후 리펙토링)
    """)
    // TODO : address name 유니크 속성 넣기 + address가 5개 이상이면 추가 생성 불가
    public ResponseEntity<ApiResponse<AddAddressResponseDto>> addAddress (
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User,
            @Valid @RequestBody AddAddressRequestDto dto,
            @Parameter(example = "req-129")
            @RequestHeader(value = "X-Request-Id", required = false) String xRequestId
    ) {
        LogPaint.sep("주소 생성 API 진입");
        Long member_id = customOAuth2User.getMemberId();

        log.info("[디버깅 목적] X-Request-Id : {}", xRequestId); // 값이 있는지 테스트

        // 현재 db에 저장한 address 반환
        Address address = addressUseCase.addAddress(member_id, dto);

        // metaData 생성
        MetaData meta = CreateMetaData.createMetaData(LocalDateTime.now(), xRequestId);

        LogPaint.sep("주소 생성 API 이탈");

        // response Dto 생성
        return ResponseEntity.ok(ApiResponse.success(
                AddAddressResponseDto.builder()
                        .memberId(member_id.toString())
                        .addressId(address.getAddressId().toString())
                        .addressName(address.getAddressName())
                        .build(), meta));
    }

    @GetMapping("orders/address")
    @Operation(summary = "주소 목록 반환", description = "현재 로그인되어 있는 사용자의 주소지 목록을 반환합니다")
    public ResponseEntity<ApiResponse<GetAddressListResponseDto>> getAddressList (
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User,
            @Parameter(example = "req-129")
            @RequestHeader(value = "X-Request-Id", required = false) String xRequestId
    ) {
        LogPaint.sep("주소 목록 반환 API 진입");
        Long member_id = customOAuth2User.getMemberId();

        log.info("[디버깅 목적] X-Request-Id : {}", xRequestId); // 값이 있는지 테스트

        List<Address> addressList = addressUseCase.getAddressList(member_id);

        // Assembler에서 ResponseDto 생성
        GetAddressListResponseDto getAddressListResponseDto
                = addressAssembler.createGetAddressListResponseDto(member_id, addressList);

        MetaData meta = CreateMetaData.createMetaData(LocalDateTime.now(), xRequestId);

        LogPaint.sep("주소 목록 반환 API 이탈");
        return ResponseEntity.ok(ApiResponse.success(getAddressListResponseDto, meta));
    }

    @DeleteMapping("orders/address/{address_id}")
    @Operation(summary = "주소 삭제", description = "주소ID가 현재 로그인한 멤버에 포함된 주소인 경우 ")
    public ResponseEntity<ApiResponse<DeleteAddressResponseDto>> deleteAddress(
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User,
            @PathVariable Long address_id,
            @Parameter(example = "req-129")
            @RequestHeader(value = "X-Request-Id", required = false) String xRequestId
    ) {
        LogPaint.sep("주소 삭제 API 진입");
        Long addresId = address_id;
        Long memberId = customOAuth2User.getMemberId();

        log.info("[디버깅 목적] X-Request-Id : {}", xRequestId); // 값이 있는지 테스트

        addressUseCase.deleteAddress(memberId, addresId);

        MetaData meta = CreateMetaData.createMetaData(LocalDateTime.now(), xRequestId);
        DeleteAddressResponseDto deleteAddressResponseDto =
                addressAssembler.createDeleteAddressResponseDto();

        LogPaint.sep("주소 삭제 API 이탈");
        return ResponseEntity.ok(ApiResponse.success(deleteAddressResponseDto, meta));
    }
}

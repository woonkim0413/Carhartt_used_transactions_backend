package com.C_platform.Member_woonkim.presentation.dto.address.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
// addAddress handler에서 사용
public record AddAddressResponseDto(
        @JsonProperty("address_id")
        @Schema(description = "주소ID", example = "주소를 식별할 수 있는 고유 값")
        String addressId,

        @JsonProperty("member_id")
        @Schema(description = "멤버ID", example = "해당 주소를 갖고 있는 member의 고유 ID")
        String memberId
) {}

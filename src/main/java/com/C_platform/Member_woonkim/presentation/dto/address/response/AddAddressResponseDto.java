package com.C_platform.Member_woonkim.presentation.dto.address.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
// addAddress handler에서 사용
public record AddAddressResponseDto(
        @JsonProperty("member_id")
        @Schema(description = "멤버ID", example = "1")
        String memberId,

        @JsonProperty("address_id")
        @Schema(description = "주소ID", example = "1")
        String addressId,

        @JsonProperty("address_name")
        @Schema(description = "주소 이름", example = "본가")
        String addressName

) {}

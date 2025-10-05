package com.C_platform.Member_woonkim.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Builder
public record GetAddressListResponseDto(
        @JsonProperty("member_id")
        @Schema(description = "멤버ID", example = "1")
        Long memberId,

        @JsonProperty("address_number")
        @Schema(description = "주소지 갯수", example = "3")
        Long addressNumber,

        @JsonProperty("address_item_list")
        @Schema(description = "주소지 목록", example = "주소1, 주소2")
        List<AddressItemDto> addressItemDtoList
) {}

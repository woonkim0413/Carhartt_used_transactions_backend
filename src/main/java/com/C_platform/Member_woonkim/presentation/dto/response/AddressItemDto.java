package com.C_platform.Member_woonkim.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record AddressItemDto(
        @JsonProperty("address_id")
        @Schema(description = "주소지 식별 ID", example = "1")
        Long addressId,

        @JsonProperty("address_name")
        @Schema(description = "주소지 이름", example = "본가")
        String addressName,

        @JsonProperty("zip_code")
        @Schema(description = "우편 주소", example = "456765")
        String zipCode,

        @JsonProperty("road_address")
        @Schema(description = "도로 주소", example = "남원시 아영면 청계길 14-1")
        String roadAddress,

        @JsonProperty("detail_address")
        @Schema(description = "상세 주소", example = "코코모 203호")
        String detailAddress
) {}

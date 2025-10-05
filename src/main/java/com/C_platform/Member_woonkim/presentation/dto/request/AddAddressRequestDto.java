package com.C_platform.Member_woonkim.presentation.dto.request;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record AddAddressRequestDto(
        @NotBlank(message = "주소지 이름을 입력해 주세요")
        @Size(max = 255, message = "주소지 이름이 너무 깁니다")
        @JsonProperty("address_name")
        @Schema(description = "주소지 이름", example = "본가")
        String addressName,

        @NotBlank(message = "우편 주소를 입력해 주세요")
        @Size(max = 20, message = "우편 번호 최대 20개의 숫자만 입력 가능합니다")
        @JsonProperty("zip_code")
        @Schema(description = "우편 주소", example = "456765")
        String zipCode,

        @NotBlank(message = "도로 주소를 입력해 주세요")
        @Size(max = 255, message = "도로 주소가 너무 깁니다")
        @JsonProperty("road_address")
        @Schema(description = "도로 주소", example = "남원시 아영면 청계길 14-1")
        String roadAddress,

        @NotBlank(message = "상세 주소를 입력해 주세요")
        @Size(max = 255, message = "상세 주소가 너무 깁니다")
        @JsonProperty("detail_address")
        @Schema(description = "상세 주소", example = "코코모 203호")
        String detailAddress
) {}

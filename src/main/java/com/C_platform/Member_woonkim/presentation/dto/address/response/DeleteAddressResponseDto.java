package com.C_platform.Member_woonkim.presentation.dto.address.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record DeleteAddressResponseDto(
        @Schema(description = "삭제 성공 유무", example = "true")
        boolean deleted
) {}

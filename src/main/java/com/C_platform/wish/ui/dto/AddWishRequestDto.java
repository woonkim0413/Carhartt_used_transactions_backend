package com.C_platform.wish.ui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddWishRequestDto {

    @JsonProperty("item_id")
    @Schema(description = "상품 ID", example = "1")
    @NotNull(message = "{item.id.notnull}")
    private Long itemId;
}

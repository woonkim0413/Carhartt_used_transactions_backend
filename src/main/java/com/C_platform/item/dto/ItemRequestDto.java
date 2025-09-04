package com.C_platform.item.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Schema(description = "상품 등록 요청 DTO")
public class ItemRequestDto {

    @Schema(description = "상품 이름", example = "아이폰 13")
    @Size(max = 100, message = "{item.name.size}") // @Validation 메시지 커스터마이징
    @NotBlank(message = "{item.name.notblank}")
    private String name;

    @Schema(description = "카테고리 ID", example = "1")
    @JsonProperty("category_id")
    @Positive(message = "{item.category.positive}")
    private int categoryId;

    @Schema(description = "사이즈")
    private Sizes sizes;

    @Schema(description = "상품 설명", example = "상태 최상급, 거의 새것")
    @NotBlank(message = "{item.description.notblank}")
    @Size(max = 1000, message = "{item.description.size}")
    private String description;

    @Schema(description = "상품 가격", example = "1000000")
    @JsonProperty("item_price")
    @Positive(message = "{item.price.positive}")
    private int itemPrice;

    @Schema(description = "직거래 여부", example = "true")
    @JsonProperty("direct_trade")
    @NotBlank(message = "{item.directtrade.notblank}")
    private boolean directTrade;
}

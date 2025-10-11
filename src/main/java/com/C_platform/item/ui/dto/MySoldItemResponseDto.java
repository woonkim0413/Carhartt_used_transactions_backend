package com.C_platform.item.ui.dto;

import com.C_platform.item.domain.Item;
import com.C_platform.item.domain.ItemStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MySoldItemResponseDto {
    @JsonProperty("item_id")
    @Schema(description = "상품 ID", example = "1")
    private Long itemId;
    @JsonProperty("product_name")
    @Schema(description = "상품명", example = "칼하트 WIP 디트로이트 자켓")
    private String productName;
    @Schema(description = "상품 가격", example = "150000")
    private Integer price;
    @Schema(description = "상품 설명", example = "이 상품은 매우 좋습니다.")
    private String description;
    @JsonProperty("sales_status")
    @Schema(description = "판매 상태", example = "SOLD_OUT")
    private ItemStatus salesStatus;

    public static MySoldItemResponseDto from(Item item) {
        return MySoldItemResponseDto.builder()
                .itemId(item.getId())
                .productName(item.getName())
                .price(item.getPrice())
                .description(item.getDescription())
                .salesStatus(item.getStatus())
                .build();
    }
}

package com.C_platform.item.ui.dto;

import com.C_platform.item.domain.Images;
import com.C_platform.item.domain.Item;
import com.C_platform.item.domain.ItemStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ItemListResponseDto {
    @JsonProperty("item_id")
    @Schema(description = "상품 ID", example = "1")
    private Long itemId;
    @JsonProperty("item_name")
    @Schema(description = "상품명", example = "칼하트 WIP 디트로이트 자켓")
    private String itemName;
    @JsonProperty("item_price")
    @Schema(description = "상품 가격", example = "150000")
    private Integer itemPrice;
    @JsonProperty("direct_trade")
    @Schema(description = "직거래 여부", example = "true")
    private boolean directTrade;
    @JsonProperty("item_status")
    @Schema(description = "상품 상태", example = "SELLING")
    private ItemStatus itemStatus;
    @Schema(description = "썸네일 이미지 URL", example = "https://example-bucket.s3.amazonaws.com/images/uuid1.jpg")
    private String thumb;

    public static ItemListResponseDto of(Item item) {
        String thumbnailUrl = item.getImages().stream()
                .findFirst()
                .map(Images::getRepresentUrl)
                .orElse(null);

        return ItemListResponseDto.builder()
                .itemId(item.getId())
                .itemName(item.getName())
                .itemPrice(item.getPrice())
                .directTrade(item.isTrade())
                .itemStatus(item.getStatus())
                .thumb(thumbnailUrl)
                .build();
    }
}

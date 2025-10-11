package com.C_platform.item.ui.dto;

import com.C_platform.item.domain.Item;
import com.C_platform.item.domain.ItemStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ItemSummaryResponseDto {

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
    private Boolean directTrade;

    @JsonProperty("item_status")
    @Schema(description = "상품 상태", example = "SELLING")
    private ItemStatus itemStatus;

    @JsonProperty("thumb")
    @Schema(description = "썸네일 이미지 URL", example = "https://example-bucket.s3.amazonaws.com/images/uuid1.jpg")
    private String thumb;

    public static ItemSummaryResponseDto of(Item item) {
        String thumbnailUrl = item.getImages().stream()
                .findFirst()
                .map(com.C_platform.item.domain.Images::getImageUrl)
                .orElse(null);

        return ItemSummaryResponseDto.builder()
                .itemId(item.getId())
                .itemName(item.getName())
                .itemPrice(item.getPrice())
                .directTrade(item.getDirectTrade())
                .itemStatus(item.getStatus())
                .thumb(thumbnailUrl)
                .build();
    }
}

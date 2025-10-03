package com.C_platform.item.ui.dto;

import com.C_platform.item.domain.Item;
import com.C_platform.item.domain.ItemStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ItemSummaryResponseDto {

    @JsonProperty("item_id")
    private Long itemId;

    @JsonProperty("item_name")
    private String itemName;

    @JsonProperty("item_price")
    private Integer itemPrice;

    @JsonProperty("direct_trade")
    private Boolean directTrade;

    @JsonProperty("item_status")
    private ItemStatus itemStatus;

    @JsonProperty("thumb")
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

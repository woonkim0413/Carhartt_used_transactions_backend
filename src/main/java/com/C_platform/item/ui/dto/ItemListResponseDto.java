package com.C_platform.item.ui.dto;

import com.C_platform.item.domain.Images;
import com.C_platform.item.domain.Item;
import com.C_platform.item.domain.ItemStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ItemListResponseDto {
    private Long item_id;
    private String item_name;
    private Integer item_price;
    private boolean direct_trade;
    private ItemStatus item_status;
    private String thumb;

    public static ItemListResponseDto of(Item item) {
        String thumbnailUrl = item.getImages().stream()
                .findFirst()
                .map(Images::getRepresentUrl)
                .orElse(null);

        return ItemListResponseDto.builder()
                .item_id(item.getId())
                .item_name(item.getName())
                .item_price(item.getPrice())
                .direct_trade(item.isTrade())
                .item_status(item.getStatus())
                .thumb(thumbnailUrl)
                .build();
    }
}

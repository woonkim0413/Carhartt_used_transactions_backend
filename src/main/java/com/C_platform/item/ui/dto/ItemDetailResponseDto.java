package com.C_platform.item.ui.dto;


import com.C_platform.item.domain.Item;
import com.C_platform.item.domain.TopItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class ItemDetailResponseDto {
    @JsonProperty("item_id")
    private Long itemId;
    @JsonProperty("item_name")
    private String itemName;
    @JsonProperty("category_ids")
    private List<Long> categoryIds;
    private SizeInfo sizes;
    @JsonProperty("item_price")
    private int itemPrice;
    @JsonProperty("direct_trade")
    private boolean directTrade;
    private String description;
    private List<ImageInfo> images;

    public static ItemDetailResponseDto of(Item item) {
        SizeInfo sizeInfo = null;
        if (item instanceof TopItem topItem) {
            sizeInfo = SizeInfo.builder()
                    .totalLength(topItem.getTotalLength())
                    .sleeve(topItem.getTopinfo().getSleeve())
                    .shoulder(topItem.getTopinfo().getShoulder())
                    .chest(topItem.getTopinfo().getChest())
                    .build();
        }

        return ItemDetailResponseDto.builder()
                .itemId(item.getId())
                .itemName(item.getName())
                .categoryIds(item.getCategories().stream()
                        .map(categoryItem -> categoryItem.getCategory().getId())
                        .collect(Collectors.toList()))
                .sizes(sizeInfo)
                .itemPrice(item.getPrice())
                .directTrade(item.isTrade())
                .description(item.getDescription())
                .images(item.getImages().stream()
                        .map(image -> ImageInfo.builder()
                                .imageId(image.getId())
                                .imageUrl(image.getImageUrl())
                                .isRepresent(image.getRepresentUrl() != null && image.getRepresentUrl().equals(image.getImageUrl()) ? 1 : 0)
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}

package com.C_platform.item.ui.dto;


import com.C_platform.item.domain.Item;
import com.C_platform.item.domain.TopItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class ItemDetailResponseDto {
    @JsonProperty("item_id")
    @Schema(description = "상품 ID", example = "1")
    private Long itemId;
    @JsonProperty("item_name")
    @Schema(description = "상품명", example = "칼하트 WIP 디트로이트 자켓")
    private String itemName;
    @JsonProperty("category_ids")
    @Schema(description = "카테고리 ID 목록", example = "[1, 2]")
    private List<Long> categoryIds;
    @Schema(description = "사이즈 정보")
    private SizeInfo sizes;
    @JsonProperty("item_price")
    @Schema(description = "상품 가격", example = "150000")
    private int itemPrice;
    @JsonProperty("direct_trade")
    @Schema(description = "직거래 여부", example = "true")
    private boolean directTrade;
    @Schema(description = "상품 설명", example = "이 상품은 매우 좋습니다.")
    private String description;
    @Schema(description = "이미지 정보 목록")
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

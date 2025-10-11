package com.C_platform.wish.ui.dto;

import com.C_platform.wish.domain.Wish;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishResponseDto {
    @Schema(description = "찜 ID", example = "1")
    private Long wishId;
    @Schema(description = "상품 ID", example = "10")
    private Long itemId;
    @Schema(description = "상품명", example = "칼하트 WIP 디트로이트 자켓")
    private String itemName;
    @Schema(description = "상품 가격", example = "150000")
    private Integer itemPrice;
    @Schema(description = "상품 이미지 URL", example = "https://example-bucket.s3.amazonaws.com/images/uuid1.jpg")
    private String itemImageUrl;

    public static WishResponseDto from(Wish wish) {
        String imageUrl = null;
        if (!wish.getItem().getImages().isEmpty()) {
            imageUrl = wish.getItem().getImages().get(0).getRepresentUrl();
        }

        return WishResponseDto.builder()
                .wishId(wish.getId())
                .itemId(wish.getItem().getId())
                .itemName(wish.getItem().getName())
                .itemPrice(wish.getItem().getPrice())
                .itemImageUrl(imageUrl)
                .build();
    }
}

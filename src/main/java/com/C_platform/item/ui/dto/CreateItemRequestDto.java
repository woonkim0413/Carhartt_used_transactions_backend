package com.C_platform.item.ui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateItemRequestDto {

    @Schema(description = "상품명", example = "멋진 티셔츠")
    @NotBlank(message = "{item.name.notblank}")
    private String name;

    @JsonProperty("category_id")
    @Schema(description = "카테고리 ID", example = "1")
    @NotNull(message = "{item.category.notnull}")
    private Integer categoryId;

    @Schema(description = "상품 사이즈")
    @NotNull(message = "{item.sizes.notnull}")
    @Valid
    private Sizes sizes;

    @Schema(description = "상품설명", example = "이 상품은 매우 좋습니다.")
    @NotBlank(message = "{item.description.notblank}")
    private String description;

    @JsonProperty("item_price")
    @Schema(description = "상품 가격", example = "25000")
    @NotNull(message = "{item.price.notnull}")
    private Integer price;

    @JsonProperty("direct_trade")
    @Schema(description = "직거래 여부", example = "true")
    @NotNull(message = "{item.directtrade.notnull}")
    private Boolean directTrade;

    @JsonProperty("image_paths")
    @Schema(description = "S3 이미지 경로 리스트" , example = "[\"https://example-bucket.s3.amazonaws.com/images/uuid1.jpg\", \"https://example-bucket.s3.amazonaws.com/images/uuid2.jpg\"]")
    @NotEmpty(message = "{image.urls.notempty}")
    private List<String> imagePaths;
}

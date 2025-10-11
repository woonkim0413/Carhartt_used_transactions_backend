package com.C_platform.item.ui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ImageInfo {
    @JsonProperty("image_id")
    @Schema(description = "이미지 ID", example = "1")
    private Long imageId;
    @JsonProperty("image_url")
    @Schema(description = "이미지 URL", example = "https://example-bucket.s3.amazonaws.com/images/uuid1.jpg")
    private String imageUrl;
    @JsonProperty("is_represent")
    @Schema(description = "대표 이미지 여부", example = "1")
    private int isRepresent;
}

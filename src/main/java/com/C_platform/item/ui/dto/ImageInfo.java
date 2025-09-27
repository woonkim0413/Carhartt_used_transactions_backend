package com.C_platform.item.ui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ImageInfo {
    @JsonProperty("image_id")
    private Long imageId;
    @JsonProperty("image_url")
    private String imageUrl;
    @JsonProperty("is_represent")
    private int isRepresent;
}

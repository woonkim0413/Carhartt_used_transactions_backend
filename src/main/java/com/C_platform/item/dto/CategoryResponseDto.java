package com.C_platform.item.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CategoryResponseDto {

    @JsonProperty("category_id")
    private Long categoryId;

    @JsonProperty("category_name")
    private String categoryName;

    @JsonProperty("p_id")
    private Long parentId;

    private List<CategoryResponseDto> children;

    public void addChildren(List<CategoryResponseDto> children) {
        this.children = children;
    }

}

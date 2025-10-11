package com.C_platform.item.ui.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CategoryResponseDto {

    @JsonProperty("category_id")
    @Schema(description = "카테고리 ID", example = "1")
    private Long categoryId;

    @JsonProperty("category_name")
    @Schema(description = "카테고리명", example = "아우터")
    private String categoryName;

    @JsonProperty("p_id")
    @Schema(description = "부모 카테고리 ID", example = "0")
    private Long parentId;

    @Schema(description = "자식 카테고리 목록")
    private List<CategoryResponseDto> children;

    public void addChildren(List<CategoryResponseDto> children) {
        this.children = children;
    }

}

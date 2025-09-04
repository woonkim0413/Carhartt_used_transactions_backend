package com.C_platform.item.dto;

import java.util.List;

public record CategoryNode(Integer p_id, Integer category_id, String category_name, List<CategoryNode> children) {
    public static CategoryNode leaf(Integer pId, Integer id, String name) {
        return new CategoryNode(pId, id, name, List.of());
    }
}
package com.C_platform.item.dto;

import java.util.List;
import java.util.Map;

public record CategoriesResponse(boolean success, List<CategoryNode> data, Map<String,Object> meta) {}
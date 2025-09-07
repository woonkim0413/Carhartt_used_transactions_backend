package com.C_platform.item.controller;

import com.C_platform.global.ApiResponse;
import com.C_platform.global.MetaData;
import com.C_platform.item.dto.CategoryResponseDto;
import com.C_platform.item.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponseDto>>> getCategories() {
        List<CategoryResponseDto> categories = categoryService.getCategories();
        MetaData meta = MetaData.builder()
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(ApiResponse.success(categories, meta));
    }
}

package com.C_platform.item.ui;

import com.C_platform.global.ApiResponse;
import com.C_platform.global.MetaData;
import com.C_platform.global.error.CategoryErrorCode;
import com.C_platform.global.error.ErrorBody;
import com.C_platform.item.applicaion.CategoryUseCase;
import com.C_platform.item.ui.dto.CategoryResponseDto;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryUseCase categoryUseCase;

    @GetMapping("/categories")
    @Operation(summary = "카테고리 조회", description = " 전체 카테고리를 조회 합니다.")
    public ResponseEntity<ApiResponse<List<CategoryResponseDto>>> getCategories() {
        Optional<List<CategoryResponseDto>> categories = categoryUseCase.getCategories();
        MetaData meta = MetaData.builder()
                .timestamp(LocalDateTime.now())
                .build();
        return categories.map(categoryResponseDtos ->
                        ResponseEntity.ok(ApiResponse.success(categoryResponseDtos, meta)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.OK)
                        .body(ApiResponse.fail(new ErrorBody<>(CategoryErrorCode.C001), meta)));
    }


}

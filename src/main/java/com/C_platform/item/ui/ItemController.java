package com.C_platform.item.ui;

import com.C_platform.global.ApiResponse;
import com.C_platform.global.MetaData;
import com.C_platform.item.applicaion.ItemUseCase;
import com.C_platform.item.domain.Item;
import com.C_platform.item.ui.dto.CreateItemRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ItemController {

    private final ItemUseCase itemUseCase;

    @PostMapping("/items")
    @Operation(summary = "상품 및 이미지 정보 저장", description = "S3에 업로드된 이미지의 URL과 상품 정보를 DB에 저장합니다.")
    public ResponseEntity<ApiResponse<?>> createItemWithImages(@RequestBody @Valid CreateItemRequestDto requestDto) {
        Item createdItem = itemUseCase.createItem(requestDto);

        MetaData meta = MetaData.builder()
                .timestamp(LocalDateTime.now())
                .build();

        Map<String, Long> response = new HashMap<>();
        response.put("item_id", createdItem.getId());
        return ResponseEntity.ok(ApiResponse.success(response, meta));
    }
}

package com.C_platform.item.ui;

import com.C_platform.Member.domain.Oauth.CustomOAuth2User;
import com.C_platform.global.ApiResponse;
import com.C_platform.global.MetaData;
import com.C_platform.item.applicaion.ItemUseCase;
import com.C_platform.item.domain.Item;
import com.C_platform.item.ui.dto.CreateItemRequestDto;
import com.C_platform.item.ui.dto.ItemDetailResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class ItemController {

    private final ItemUseCase itemUseCase;

    @PostMapping("/items")
    @Operation(summary = "상품 및 이미지 정보 저장", description = "S3에 업로드된 이미지의 URL과 상품 정보를 DB에 저장합니다.")
    public ResponseEntity<ApiResponse<?>> createItemWithImages(@RequestBody @Valid CreateItemRequestDto requestDto ,
                                                               @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {

        extracted(customOAuth2User);

        Long memberId = customOAuth2User.getMemberId();

        Item createdItem = itemUseCase.createItem(requestDto , memberId);

        Map<String, Long> response = new HashMap<>();
        response.put("item_id", createdItem.getId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdItem.getId())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.success(response, getMetaData()));
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "상품 정보 수정", description = "상품 정보를 수정합니다.")
    public ResponseEntity<ApiResponse<?>> updateItem(@PathVariable Long itemId,
                                                   @RequestBody @Valid com.C_platform.item.ui.dto.UpdateItemRequestDto requestDto,
                                                   @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {

        extracted(customOAuth2User);

        Long memberId = customOAuth2User.getMemberId();

        Item updatedItem = itemUseCase.updateItem(itemId, memberId, requestDto);

        Map<String, Object> response = new HashMap<>();
        response.put("updated", true);
        response.put("item_id", updatedItem.getId());
        return ResponseEntity.ok().body(ApiResponse.success(response, getMetaData()));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "상품 정보 삭제", description = "상품 정보를 삭제합니다.")
    public ResponseEntity<ApiResponse<?>> deleteItem(@PathVariable Long itemId,
                                                   @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {

        extracted(customOAuth2User);

        Long memberId = customOAuth2User.getMemberId();

        itemUseCase.deleteItem(itemId, memberId);

        Map<String, Object> response = new HashMap<>();
        response.put("deleted", true);
        response.put("item_id", itemId);

        return ResponseEntity.ok().body(ApiResponse.success(response, getMetaData()));
    }

    @GetMapping("/items/{itemId}")
    @Operation(summary = "상품 상세 정보 조회", description = "상품의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<?>> findItemDetail(@PathVariable Long itemId) {
        ItemDetailResponseDto itemDetail = itemUseCase.findItemDetailById(itemId);
        return ResponseEntity.ok().body(ApiResponse.success(itemDetail, getMetaData()));
    }


    /**
     * 메타데이터 생성
     * @return
     */
    private static MetaData getMetaData() {
        return MetaData.builder()
                .timestamp(LocalDateTime.now())
                .build();
    }


    /**
     * 인증된 사용자 정보 검증
     * @param customOAuth2User
     */
    private static void extracted(CustomOAuth2User customOAuth2User) {
        if (customOAuth2User == null) {
            throw new IllegalStateException("인증된 사용자 정보를 찾을 수 없습니다.");
        }
    }
}

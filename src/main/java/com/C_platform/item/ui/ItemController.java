package com.C_platform.item.ui;

import com.C_platform.Member_woonkim.domain.entitys.CustomOAuth2User;
import com.C_platform.Member_woonkim.utils.CreateMetaData;
import com.C_platform.global.ApiResponse;
import com.C_platform.global.MetaData;
import com.C_platform.images.application.ImageUseCase;
import com.C_platform.images.domain.ImagePreSignedUrlRequestDto;
import com.C_platform.images.domain.ImagePreSignedUrlResponseDto;
import com.C_platform.item.application.ItemUseCase;
import com.C_platform.item.domain.Item;
import com.C_platform.item.ui.dto.*;
import com.C_platform.global.PageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import software.amazon.awssdk.http.SdkHttpMethod;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Item", description = "아이템 API")
public class ItemController {

    private final ItemUseCase itemUseCase;
    private final ImageUseCase imageUseCase;

    @PostMapping("/items")
    @Operation(summary = "상품 및 이미지 정보 저장", description = "S3에 업로드된 이미지의 URL과 상품 정보를 DB에 저장합니다.")
    public ResponseEntity<ApiResponse<?>> createItemWithImages(@RequestBody @Valid CreateItemRequestDto requestDto ,
                                                               @RequestHeader("X-Request-Id") String x_request_id,
                                                               @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {

        extracted(customOAuth2User);

        Long memberId = customOAuth2User.getMemberId();

        Item createdItem = itemUseCase.createItem(requestDto , memberId);

        //        Map<String, Long> response = new HashMap<>();
        //        response.put("item_id", createdItem.getId());
        Map<String, Long> response = Map.of("item_id", createdItem.getId());

//        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
//                .path("/{id}")
//                .buildAndExpand(createdItem.getId())
//                .toUri();
        return ResponseEntity.ok().body(ApiResponse.success(response, getMetaData(x_request_id)));
    }

    @PostMapping("/items/presigned-url")
    @Operation(summary = "상품 업로드 URL 생성", description = "상품 이미지 업로드를 위한 사전 서명된 URL을 생성합니다.")
    public ResponseEntity<ApiResponse<ImagePreSignedUrlResponseDto>> generateUrl(@RequestBody ImagePreSignedUrlRequestDto requestDto,
                                                                                 @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {

        String userId = customOAuth2User.getMemberId().toString();
        ImagePreSignedUrlResponseDto preSignedUrl = imageUseCase.createPreSignedUrls(userId, SdkHttpMethod.PUT, requestDto);
        MetaData meta = CreateMetaData.createMetaData(LocalDateTime.now());
        return ResponseEntity.ok(ApiResponse.success(preSignedUrl, meta));
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "상품 정보 수정", description = "상품 정보를 수정합니다.")
    public ResponseEntity<ApiResponse<?>> updateItem(@PathVariable Long itemId,
                                                     @RequestBody @Valid UpdateItemRequestDto requestDto,
                                                     @AuthenticationPrincipal CustomOAuth2User customOAuth2User,
                                                     @RequestHeader("X-Request-Id") String x_request_id) {

        extracted(customOAuth2User);

        Long memberId = customOAuth2User.getMemberId();

        Item updatedItem = itemUseCase.updateItem(itemId, memberId, requestDto);

        //        Map<String, Object> response = new HashMap<>();
        //        response.put("updated", true);
        //        response.put("item_id", updatedItem.getId());
        Map<String, Object> response = Map.of("updated", true, "item_id", updatedItem.getId());
        return ResponseEntity.ok().body(ApiResponse.success(response, getMetaData(x_request_id)));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "상품 정보 삭제", description = "상품 정보를 삭제합니다.")
    public ResponseEntity<ApiResponse<?>> deleteItem(@PathVariable Long itemId,
                                                     @AuthenticationPrincipal CustomOAuth2User customOAuth2User,
                                                     @RequestHeader("X-Request-Id") String x_request_id) {

        extracted(customOAuth2User);

        Long memberId = customOAuth2User.getMemberId();

        itemUseCase.deleteItem(itemId, memberId);

        //        Map<String, Object> response = new HashMap<>();
        //        response.put("deleted", true);
        //        response.put("item_id", itemId);
        Map<String, Object> response = Map.of("deleted", true, "item_id", itemId);

        return ResponseEntity.ok().body(ApiResponse.success(response, getMetaData(x_request_id)));
    }

    @GetMapping("/items/{itemId}")
    @Operation(summary = "상품 상세 정보 조회", description = "상품의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<?>> findItemDetail(@PathVariable Long itemId,
                                                         @RequestHeader("X-Request-Id") String x_request_id) {

        ItemDetailResponseDto itemDetail = itemUseCase.findItemDetailById(itemId);
        return ResponseEntity.ok().body(ApiResponse.success(itemDetail, getMetaData(x_request_id)));
    }

    @GetMapping("/items")
    @Operation(summary = "상품 목록 조회", description = "상품 목록을 페이지네이션하여 조회합니다. 키워드 검색이 가능합니다.")
    public ResponseEntity<ApiResponse<PageResponseDto<ItemListResponseDto>>> findItems(@RequestParam(required = false) String keyword,
                                                                                       @RequestHeader("X-Request-Id") String x_request_id,
                                                                                       Pageable pageable) {
        Page<ItemListResponseDto> items = itemUseCase.findItems(keyword, pageable);
        PageResponseDto<ItemListResponseDto> responseDto = PageResponseDto.of(items);
        return ResponseEntity.ok().body(ApiResponse.success(responseDto, getMetaData(x_request_id)));
    }


    //TODO 회원이 본인이 판매 및 판매완료 한 제품 정보 표시
    @GetMapping("/items/mysolditems")
    @Operation(summary = "회원 본인이 판매 및 판매 완료한 상품 목록 조회", description = "인증된 회원이 판매했거나 판매 완료한 상품 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<?>> getMySoldItems(@AuthenticationPrincipal CustomOAuth2User customOAuth2User ,
                                                         @RequestHeader("X-Request-Id") String x_request_id) {

        extracted(customOAuth2User);

        Long memberId = customOAuth2User.getMemberId();

        List<MySoldItemResponseDto> mySoldItems = itemUseCase.getMySoldItems(memberId);

        return ResponseEntity.ok().body(ApiResponse.success(mySoldItems, getMetaData(x_request_id)));
    }


    /**
     * 메타데이터 생성
     * @return
     */
    private static MetaData getMetaData(String requestId) {
        return MetaData.builder()
                .timestamp(LocalDateTime.now())
                .xRequestId(requestId)
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
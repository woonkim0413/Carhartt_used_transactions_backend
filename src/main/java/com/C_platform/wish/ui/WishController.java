package com.C_platform.wish.ui;

import com.C_platform.Member_woonkim.domain.entitys.CustomOAuth2User;
import com.C_platform.global.ApiResponse;
import com.C_platform.global.MetaData;
import com.C_platform.wish.applicaion.WishUseCase;
import com.C_platform.wish.ui.dto.AddWishRequestDto;
import com.C_platform.wish.ui.dto.WishResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Wish", description = "나의 찜 API")
public class WishController {

    private final WishUseCase wishUseCase;

    @PostMapping("/wishes")
    @Operation(summary = "아이템 찜하기", description = "특정 아이템을 회원의 찜 목록에 추가합니다.")
    public ResponseEntity<ApiResponse<?>> addWish(@Valid @RequestBody AddWishRequestDto addWishRequestDto,
                                                  @AuthenticationPrincipal CustomOAuth2User customOAuth2User,
                                                  @RequestHeader("X-Request-Id") String x_request_id) {

        extracted(customOAuth2User);
        Long memberId = customOAuth2User.getMemberId();

        wishUseCase.addWish(memberId, addWishRequestDto.getItemId());

        return ResponseEntity.ok().body(ApiResponse.success(Map.of("message", "찜 등록에 성공했습니다."), getMetaData(x_request_id)));
    }

    @DeleteMapping("/wishes/{itemId}")
    @Operation(summary = "아이템 찜 해제", description = "특정 아이템을 회원의 찜 목록에서 제거합니다.")
    public ResponseEntity<ApiResponse<?>> removeWish(@Schema(description = "상품 ID") @PathVariable Long itemId,
                                                     @RequestHeader("X-Request-Id") String x_request_id,
                                                     @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {

        extracted(customOAuth2User);
        Long memberId = customOAuth2User.getMemberId();

        wishUseCase.removeWish(memberId, itemId);

        return ResponseEntity.ok().body(ApiResponse.success(Map.of("message", "찜 목록에서 삭제되었습니다."), getMetaData(x_request_id)));
    }

    @GetMapping("/wishes")
    @Operation(summary = "회원의 찜 목록 조회", description = "인증된 회원의 찜 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<WishResponseDto>>> getMyWishlist(@AuthenticationPrincipal CustomOAuth2User customOAuth2User,
                                                                            @RequestHeader("X-Request-Id") String x_request_id) {

        extracted(customOAuth2User);
        Long memberId = customOAuth2User.getMemberId();

        List<WishResponseDto> wishlist = wishUseCase.getMyWishlist(memberId);

        return ResponseEntity.ok().body(ApiResponse.success(wishlist, getMetaData(x_request_id)));
    }

    @GetMapping("/wishes/{itemId}/status")
    @Operation(summary = "아이템 찜 상태 확인", description = "특정 아이템이 현재 회원의 찜 목록에 있는지 확인합니다.")
    public ResponseEntity<ApiResponse<?>> checkWishStatus(@Schema(description = "상품 ID") @PathVariable Long itemId,
                                                          @AuthenticationPrincipal CustomOAuth2User customOAuth2User,
                                                          @RequestHeader("X-Request-Id") String x_request_id) {

        extracted(customOAuth2User);
        Long memberId = customOAuth2User.getMemberId();

        boolean isWished = wishUseCase.isItemWished(memberId, itemId);

        return ResponseEntity.ok().body(ApiResponse.success(Map.of("isWished", isWished), getMetaData(x_request_id)));
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



    private static void extracted(CustomOAuth2User customOAuth2User) {
        if (customOAuth2User == null) {
            throw new IllegalStateException("인증된 사용자 정보를 찾을 수 없습니다.");
        }
    }
}

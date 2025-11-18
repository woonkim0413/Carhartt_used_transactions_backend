package com.C_platform.Member_woonkim.presentation.controller;

import com.C_platform.Member_woonkim.application.useCase.MyPageUseCase;
import com.C_platform.Member_woonkim.domain.value.CustomOAuth2User;
import com.C_platform.Member_woonkim.presentation.dto.myPage.request.UpdateProfileImageRequestDto;
import com.C_platform.Member_woonkim.utils.CreateMetaData;
import com.C_platform.global.ApiResponse;
import com.C_platform.global.MetaData;
import com.C_platform.images.application.ImageUseCase;
import com.C_platform.images.domain.ImagePreSignedUrlRequestDto;
import com.C_platform.images.domain.ImagePreSignedUrlResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.http.SdkHttpMethod;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/v1/myPage/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ImageUseCase imageUseCase;
    private final MyPageUseCase myPageUseCase;

    @PostMapping("/presigned-url")
    @Operation(summary = "프로필 이미지 업로드 URL 생성", description = "프로필 이미지 업로드를 위한 사전 서명된 URL을 생성합니다.")
    public ResponseEntity<ApiResponse<ImagePreSignedUrlResponseDto>> generatePresignedUrl(
            @RequestBody ImagePreSignedUrlRequestDto requestDto,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {

        String userId = customOAuth2User.getMemberId().toString();
        ImagePreSignedUrlResponseDto preSignedUrl = imageUseCase.createPreSignedUrls(userId, SdkHttpMethod.PUT, requestDto);
        MetaData meta = CreateMetaData.createMetaData(LocalDateTime.now());

        return ResponseEntity.ok(ApiResponse.success(preSignedUrl, meta));
    }

    @PutMapping("/image")
    @Operation(summary = "프로필 이미지 변경", description = "사용자의 프로필 이미지를 변경합니다.")
    public ResponseEntity<ApiResponse<Void>> updateProfileImage(
            @RequestBody @Valid UpdateProfileImageRequestDto dto,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
        Long memberId = customOAuth2User.getMemberId();
        myPageUseCase.updateProfileImage(memberId, dto.profileImageUrl());
        return ResponseEntity.ok(ApiResponse.success(null, CreateMetaData.createMetaData(LocalDateTime.now())));
    }
}

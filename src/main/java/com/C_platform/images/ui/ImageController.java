package com.C_platform.images.ui;

import com.C_platform.exception.InvalidImageExtensionException;
import com.C_platform.global.ApiResponse;
import com.C_platform.global.MetaData;
import com.C_platform.global.error.ImageErrorCode;
import com.C_platform.images.applicaion.ImageUseCase;
import com.C_platform.images.domain.ImagePreSignedUrlListRequestDto;
import com.C_platform.images.domain.ImagePreSignedUrlRequestDto;
import com.C_platform.images.domain.ImagePreSignedUrlResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.http.SdkHttpMethod;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ImageController {


    private final ImageUseCase imageUseCase;
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png");


    @PostMapping("/image/presignedurl")
    @Operation(summary = "이미지 업로드 URL 생성", description = "이미지 업로드를 위한 사전 서명된 URL 을 생성 합니다.")
    public ResponseEntity<ApiResponse<ImagePreSignedUrlResponseDto>> generateUrl(@RequestBody ImagePreSignedUrlRequestDto requestDto,
                                                                                 @CookieValue("JSESSIONID") String sessionId, HttpServletRequest request) {

        validateExtension(requestDto.getExtension());

        HttpSession session = request.getSession(false);

        String userId = "ANONYMOUS";
        ImagePreSignedUrlResponseDto preSignedUrls = imageUseCase.createPreSignedUrls(userId,SdkHttpMethod.PUT,requestDto);
        MetaData meta = MetaData.builder()
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(ApiResponse.success(preSignedUrls, meta));
    }

    private void validateExtension(String extension) {
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new InvalidImageExtensionException(ImageErrorCode.I001);
        }
    }

//    @PostMapping("/image/presignedurls") // 복수형으로 변경
//    @Operation(summary = "여러 이미지 업로드 URL 생성", description = "여러 이미지 업로드를 위한 사전 서명된 URL 목록을 생성합니다.")
//    public ResponseEntity<ApiResponse<List<ImagePreSignedUrlResponseDto>>> generateUrls(@RequestBody ImagePreSignedUrlListRequestDto requestDto, // 새로운 요청 DTO 사용
//                                                                                        @CookieValue("JSESSIONID") String sessionId, HttpServletRequest request) {
//
//        HttpSession session = request.getSession(false);
//        String userId = "ANONYMOUS"; // 추후 실제 사용자 ID로 변경
//
//        // UseCase를 호출하여 여러 URL을 한 번에 생성
//        List<ImagePreSignedUrlResponseDto> preSignedUrls = imageUseCase.createPreSignedUrls(userId, SdkHttpMethod.PUT, requestDto);
//
//        MetaData meta = MetaData.builder()
//                .timestamp(LocalDateTime.now())
//                .build();
//
//        return ResponseEntity.ok(ApiResponse.success(preSignedUrls, meta));
//    }
}

package com.C_platform.images.domain;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


/**
 * 이미지 업로드 URL 응답 DTO
 */
@Getter
@Setter
@Builder
public class ImagePreSignedUrlResponseDto {
    private String originalFileName;
    private String preSignedUrl;
    private String filePath;
}

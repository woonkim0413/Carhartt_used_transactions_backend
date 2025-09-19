package com.C_platform.images.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import lombok.Getter;
import lombok.Setter;

/**
 * 이미지 업로드 요청 DTO
 */
@Getter
@Setter
public class ImagePreSignedUrlRequestDto {

    @Schema(description = "원본파일 이름", example = "example.jpg")
    private String originalFileName;

    @Max(value = 300, message = "파일 크기는 300KB 이하여야 합니다.")
    @Schema(description = "파일 크기 (KB)", example = "300")
    private Long fileSize;

    @Schema(description = "파일 확장자", example = "jpg")
    private String extension;
}

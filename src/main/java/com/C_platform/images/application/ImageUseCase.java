package com.C_platform.images.application;

import com.C_platform.config.FileConfig;
import com.C_platform.images.domain.ImagePreSignedUrlListRequestDto;
import com.C_platform.images.domain.ImagePreSignedUrlRequestDto;
import com.C_platform.images.domain.ImagePreSignedUrlResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ConditionalOnProperty(name = "cloud.aws.s3.enabled", havingValue = "true")
@Service
@RequiredArgsConstructor
public class ImageUseCase {

    @Value("${aws.bucket}")
    private String bucketName;

    private final S3Presigner s3Presigner;

    private final FileConfig fileConfig;

    /**
     * 이미지 업로드 URL 생성
     * @param userId
     * @param requestDto
     * @return
     */
    public ImagePreSignedUrlResponseDto createPreSignedUrls(String userId ,
                                                            SdkHttpMethod method ,
                                                            ImagePreSignedUrlRequestDto requestDto) {
        String originalFileName = requestDto.getOriginalFileName();

        // 1. 파일명 중복을 피하기 위해 UUID 추가
        String uniqueFileName = UUID.randomUUID() + "_" + requestDto.getOriginalFileName();

        // 2. "users/{사용자ID}/{고유파일명}" 형태로 최종 경로(Key) 생성
        String filePath = "users/" + userId + "/" + uniqueFileName;

        return ImagePreSignedUrlResponseDto.builder()
                .originalFileName(originalFileName)
                .preSignedUrl(generatePutPresignedUrl(filePath))
                .filePath(filePath)
                .build();
    }


    /**
     * 이미지 업로드 용 사전 서명된 URL 생성
     * @param filePath
     * @return
     */
    private String generatePutPresignedUrl(String filePath) {
        PutObjectRequest.Builder putObjectRequestBuilder = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(filePath);

        PutObjectRequest putObjectRequest = putObjectRequestBuilder.build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return presignedRequest.url().toString();
    }

    /**
     * 여러 이미지 업로드 URL 생성
     * @param userId
     * @param method
     * @param requestDto
     * @return
     */
    public List<ImagePreSignedUrlResponseDto> createPreSignedUrls(String userId, SdkHttpMethod method, ImagePreSignedUrlListRequestDto requestDto) {
        return requestDto.getOriginalFileNames().stream()
                .map(originalFileName -> {

                    String uniqueFileName = UUID.randomUUID() + "_" + originalFileName;

                    String filePath = "users/" + userId + "/" + uniqueFileName;

                    String preSignedUrl = generatePutPresignedUrl(filePath);


                    return ImagePreSignedUrlResponseDto.builder()
                            .originalFileName(originalFileName)
                            .preSignedUrl(preSignedUrl)
                            .filePath(filePath)
                            .build();
                })
                .collect(Collectors.toList());
    }

}

package com.C_platform.images.domain;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ImagePreSignedUrlListRequestDto {
    private List<String> originalFileNames;
}

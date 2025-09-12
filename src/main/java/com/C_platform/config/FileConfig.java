package com.C_platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "file")
public class FileConfig {

    /**
     * 파일 최대 크기 KB 단위
     */
    private Long maxSize;

    /**
     * 허용 확장자
     */
    private List<String> extensions;

    public Long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Long maxSize) {
        this.maxSize = maxSize;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public void setExtensions(List<String> extensions) {
        this.extensions = extensions;
    }
}

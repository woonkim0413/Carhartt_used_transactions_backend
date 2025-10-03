package com.C_platform.global;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class PageResponseDto<T> {
    private List<T> content;
    private int page;
    private int size;
    private long total_elements;
    private int total_pages;

    public static <T> PageResponseDto<T> of(Page<T> page) {
        return PageResponseDto.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .total_elements(page.getTotalElements())
                .total_pages(page.getTotalPages())
                .build();
    }
}

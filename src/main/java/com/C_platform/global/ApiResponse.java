package com.C_platform.global;

import com.C_platform.global.error.ErrorBody;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorBody<?> error;
    private final MetaData meta;

    public static <T> ApiResponse<T> success(T data, MetaData meta) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .meta(meta)
                .build();
    }

    public static <T> ApiResponse<T> fail(ErrorBody<?> error, MetaData meta) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .meta(meta)
                .build();
    }
}
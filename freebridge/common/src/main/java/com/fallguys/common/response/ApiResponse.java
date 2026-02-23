package com.fallguys.common.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fallguys.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

public record ApiResponse<T>(
        @JsonIgnore
        HttpStatus httpStatus,
        boolean success,
        @Nullable T data,
        @Nullable ErrorResponse error
) {
    public static <T> ApiResponse<T> ok(@Nullable final T data) {
        return new ApiResponse<>(HttpStatus.OK, true, data, null);
    }

    public static <T> ApiResponse<T> created(@Nullable final T data) {
        return new ApiResponse<>(HttpStatus.CREATED, true, data, null);
    }

    public static ApiResponse<?> fail(ErrorCode errorCode) {
        return new ApiResponse<>(
                HttpStatus.valueOf(errorCode.getStatus()),
                false,
                null,
                new ErrorResponse(errorCode.getCode(), errorCode.getMessage())
        );
    }

    public record ErrorResponse(String code, String message) { }
}
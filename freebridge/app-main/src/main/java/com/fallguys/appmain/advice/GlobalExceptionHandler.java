package com.fallguys.appmain.advice;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException e) {
        log.warn("Business Exception : {}", e.getErrorCode().getMessage());
        return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                .body(ApiResponse.fail(e.getErrorCode()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        log.warn("Validation Failed");
        // ApiResponse<?>를 Object로 취급하여 반환
        Object body = ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleRemainingException(Exception e) {
        log.error("Unexpected Exception : ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

        if (body == null || !(body instanceof ApiResponse)) {
            body = ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }
}
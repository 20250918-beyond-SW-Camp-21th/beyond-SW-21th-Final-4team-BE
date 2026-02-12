package com.fallguys.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 공통
    INVALID_INPUT_VALUE(400, "C001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(500, "C002", "서버 내부 오류가 발생했습니다."),

    RAG_PROCESSING_ERROR(500, "R001", "AI 응답 처리 중 오류가 발생했습니다."),
    PYTHON_SERVER_UNREACHABLE(503, "R002", "AI 엔진 서버에 연결할 수 없습니다.");

    private final int status;
    private final String code;
    private final String message;

    public org.springframework.http.HttpStatus getHttpStatus() {
        return org.springframework.http.HttpStatus.valueOf(status);
    }
}
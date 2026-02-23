package com.fallguys.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 공통
    INVALID_INPUT_VALUE(400, "C001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(500, "C002", "서버 내부 오류가 발생했습니다."),

    // User / Auth
    EMAIL_DUPLICATE(409, "U001", "이미 사용 중인 이메일입니다."),
    USER_NOT_FOUND(404, "U002", "사용자를 찾을 수 없습니다."),
    INVALID_PASSWORD(401, "U003", "비밀번호가 일치하지 않습니다."),
    EMAIL_NOT_VERIFIED(403, "U004", "이메일 인증이 완료되지 않았습니다."),
    INVALID_VERIFICATION_CODE(400, "U005", "인증 코드가 올바르지 않거나 만료되었습니다."),
    VERIFICATION_CODE_EXPIRED(400, "U006", "인증 코드가 만료되었습니다."),

    // AI
    RAG_PROCESSING_ERROR(500, "R001", "AI 응답 처리 중 오류가 발생했습니다."),
    PYTHON_SERVER_UNREACHABLE(503, "R002", "AI 엔진 서버에 연결할 수 없습니다."),

    // Recruitment / JobPosting
    JOB_POSTING_NOT_FOUND(404, "JP001", "공고를 찾지 못 했습니다."),
    JOB_POSTING_FORBIDDEN(403, "JP002", "권한이 없습니다."),
    JOB_POSTING_ALREADY_DELETED(409, "JP003", "이미 삭제된 공고입니다."),
    ONLY_EMPLOYER_ALLOWED(403, "JP004", "employer만 가능합니다.");

    private final int status;
    private final String code;
    private final String message;

    public org.springframework.http.HttpStatus getHttpStatus() {
        return org.springframework.http.HttpStatus.valueOf(status);
    }
}

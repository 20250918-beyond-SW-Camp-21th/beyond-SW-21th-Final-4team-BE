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
    ONLY_EMPLOYER_ALLOWED(403, "JP004", "employer만 가능합니다."),
    ONLY_FREELANCER_ALLOWED(403, "JP005", "freelancer만 가능합니다."),
    JOB_POSTING_HEADCOUNT_FULL(409, "JP006", "모집 인원이 모두 충족되었습니다."),

    // Contract
    CONTRACT_NOT_FOUND(404, "CON001", "계약을 찾을 수 없습니다."),
    CONTRACT_NOT_IN_PROGRESS(400, "CON002", "진행 중인 계약만 완료 처리할 수 있습니다."),
    CONTRACT_CANNOT_REJECT(400, "CON003", "이미 진행 중이거나 완료된 계약은 거절할 수 없습니다."),
    CONTRACT_FORBIDDEN(403, "CON004", "해당 계약에 접근할 권한이 없습니다."),
    CONTRACT_NOT_ACTIVATABLE(400, "CON005", "양측 서명이 완료되고 대기 중인 계약만 활성화할 수 있습니다."),

    // Payment
    PAYMENT_NOT_FOUND(404, "PAY001", "결제 정보를 찾을 수 없습니다."),
    PAYMENT_AMOUNT_MISMATCH(400, "PAY002", "결제 금액이 계약 금액과 일치하지 않습니다."),
    PAYMENT_ALREADY_PROCESSED(409, "PAY003", "이미 처리된 결제입니다."),
    PAYMENT_FAILED(400, "PAY004", "결제 처리에 실패했습니다."),
    BILLING_KEY_NOT_FOUND(404, "PAY005", "빌링키를 찾을 수 없습니다."),
    SETTLEMENT_NOT_FOUND(404, "PAY006", "정산 내역을 찾을 수 없습니다."),
    SETTLEMENT_FORBIDDEN(403, "PAY007", "해당 정산 내역에 접근할 권한이 없습니다."),
    WALLET_NOT_FOUND(404, "PAY008", "지갑을 찾을 수 없습니다.");

    private final int status;
    private final String code;
    private final String message;

    public org.springframework.http.HttpStatus getHttpStatus() {
        return org.springframework.http.HttpStatus.valueOf(status);
    }
}

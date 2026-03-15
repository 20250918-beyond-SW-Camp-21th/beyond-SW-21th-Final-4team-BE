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
    UNAUTHORIZED(401, "U007", "Authentication required."),
    EMAIL_NOT_VERIFIED(403, "U004", "이메일 인증이 완료되지 않았습니다."),
    INVALID_VERIFICATION_CODE(400, "U005", "인증 코드가 올바르지 않거나 만료되었습니다."),
    VERIFICATION_CODE_EXPIRED(400, "U006", "인증 코드가 만료되었습니다."),

    // AI
    RAG_PROCESSING_ERROR(500, "R001", "AI 응답 처리 중 오류가 발생했습니다."),
    PYTHON_SERVER_UNREACHABLE(503, "R002", "AI 파이썬 서버에 연결할 수 없습니다."),

    // Recruitment / JobPosting
    JOB_POSTING_NOT_FOUND(404, "JP001", "공고를 찾을 수 없습니다."),
    JOB_POSTING_FORBIDDEN(403, "JP002", "권한이 없습니다."),
    JOB_POSTING_ALREADY_DELETED(409, "JP003", "이미 삭제된 공고입니다."),
    ONLY_EMPLOYER_ALLOWED(403, "JP004", "employer만 접근 가능합니다."),
    ONLY_FREELANCER_ALLOWED(403, "JP005", "freelancer만 접근 가능합니다."),
    JOB_POSTING_HEADCOUNT_FULL(409, "JP006", "모집 인원이 모두 충족되었습니다."),
    PROJECT_NOT_FOUND(404, "PJ001", "해당 프로젝트를 찾을 수 없습니다."),
    PROJECT_ALREADY_COMPLETED(409, "PJ002", "이미 완료된 프로젝트입니다."),

    // Contract
    CONTRACT_NOT_FOUND(404, "CON001", "계약을 찾을 수 없습니다."),
    CONTRACT_NOT_IN_PROGRESS(400, "CON002", "진행 중인 계약만 처리할 수 있습니다."),
    CONTRACT_CANNOT_REJECT(400, "CON003", "이미 진행 중이거나 완료된 계약은 거절할 수 없습니다."),
    CONTRACT_FORBIDDEN(403, "CON004", "해당 계약에 대한 권한이 없습니다."),
    CONTRACT_NOT_ACTIVATABLE(400, "CON005", "서명이 완료되고 대기 중인 계약만 활성화할 수 있습니다."),

    // Payment
    PAYMENT_NOT_FOUND(404, "PAY001", "결제 정보를 찾을 수 없습니다."),
    PAYMENT_AMOUNT_MISMATCH(400, "PAY002", "결제 금액이 계약 금액과 일치하지 않습니다."),
    PAYMENT_ALREADY_PROCESSED(409, "PAY003", "이미 처리된 결제입니다."),
    PAYMENT_FAILED(400, "PAY004", "결제 처리에 실패했습니다."),
    BILLING_KEY_NOT_FOUND(404, "PAY005", "빌링키를 찾을 수 없습니다."),
    SETTLEMENT_NOT_FOUND(404, "PAY006", "정산 내역을 찾을 수 없습니다."),
    SETTLEMENT_FORBIDDEN(403, "PAY007", "해당 정산 내역에 대한 권한이 없습니다."),
    WALLET_NOT_FOUND(404, "PAY008", "지갑을 찾을 수 없습니다."),

    // Subscription
    SUBSCRIPTION_INVALID_REQUEST(400, "SUB001", "구독 요청 값이 유효하지 않습니다."),
    SUBSCRIPTION_INVALID_PLAN(400, "SUB002", "유효하지 않은 플랜 값입니다."),
    SUBSCRIPTION_SAME_PLAN(400, "SUB003", "현재와 동일한 플랜으로는 변경할 수 없습니다."),
    SUBSCRIPTION_BILLING_KEY_REQUIRED(400, "SUB004", "유료 플랜 변경에는 billingKey가 필요합니다."),
    SUBSCRIPTION_CANCEL_REQUIRED(400, "SUB005", "무료 플랜 전환은 구독 취소 기능을 이용해주세요."),
    SUBSCRIPTION_ALREADY_BASIC(400, "SUB006", "이미 BASIC 플랜을 사용 중입니다."),

    // Resume
    RESUME_NOT_FOUND(404, "RES001", "이력서를 찾을 수 없습니다.");

    private final int status;
    private final String code;
    private final String message;

    public org.springframework.http.HttpStatus getHttpStatus() {
        return org.springframework.http.HttpStatus.valueOf(status);
    }
}

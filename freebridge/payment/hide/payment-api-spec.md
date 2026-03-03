# Payment API 명세서

---

# Employer Settlement

---

## 고용주 정산 목록 조회

`GET /api/v1/settlements/employer`

---

용도: 로그인한 고용주의 정산 목록을 조회합니다. 상태, 날짜 범위, 검색어, 정렬 기준으로 필터링 가능하며 페이지네이션을 지원합니다.

단일/페이징: 페이징

Query Parameters:
```
status    : "ALL" | "ISSUED" | "PAID" | "DISBURSED" | "CANCELLED"  (default: "ALL")
dateRange : "ALL" | "THIS_MONTH" | "LAST_MONTH" | "LAST_3_MONTHS"  (default: "ALL")
search    : string (프로젝트명 또는 프리랜서 이름)
sort      : "DUE_DATE_ASC" | "DUE_DATE_DESC" | "AMOUNT_DESC"       (default: "DUE_DATE_ASC")
page      : int  (default: 1)
size      : int  (default: 10)
```

JSON 형식:

```json
// 요청
// 없음 — Header: X-User-Id, Query Parameters로 전달
// 예시: GET /api/v1/settlements/employer?status=PAID&page=1&size=10

// 성공 응답 (200 OK)
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "contractId": 101,
        "projectName": "SaaS 대시보드 개발",
        "freelancerName": "이지연",
        "billingAmount": 1666666,
        "platformFee": 166666,
        "totalPayment": 1833332,
        "installmentNumber": 1,
        "status": "PAID",
        "invoicePdfUrl": "https://s3.../invoice_1.pdf",
        "dueDate": "2026-03-25",
        "paidDate": "2026-02-27"
      }
    ],
    "totalElements": 6,
    "totalPages": 1,
    "currentPage": 1
  },
  "error": null
}

// 실패 응답
// 401 Unauthorized — 인증 토큰 없음
{
  "success": false,
  "data": null,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "인증이 필요합니다."
  }
}
```

---

## 고용주 정산 통계 조회

`GET /api/v1/settlements/employer/summary`

---

용도: 로그인한 고용주의 전체 정산 집계 데이터(총 지급액, 건수 등)를 조회합니다.

단일/페이징: 단일

JSON 형식:

```json
// 요청
// 없음 — Header: X-User-Id

// 성공 응답 (200 OK)
{
  "success": true,
  "data": {
    "totalPaidAmount": 10999992,
    "totalDisbursedAmount": 7333328,
    "paidCount": 3,
    "disbursedCount": 2,
    "cancelledCount": 0
  },
  "error": null
}

// 실패 응답
// 401 Unauthorized
{
  "success": false,
  "data": null,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "인증이 필요합니다."
  }
}
```

---

## 다음 정산 예정 조회

`GET /api/v1/settlements/employer/next`

---

용도: 고용주의 가장 가까운 미지급 정산 회차를 조회합니다. 모든 회차가 지급 완료되었거나 해당 건이 없으면 null을 반환합니다.

단일/페이징: 단일

JSON 형식:

```json
// 요청
// 없음 — Header: X-User-Id

// 성공 응답 (200 OK)
{
  "success": true,
  "data": {
    "id": 3,
    "contractId": 101,
    "projectName": "SaaS 대시보드 개발",
    "freelancerName": "이지연",
    "billingAmount": 1666666,
    "platformFee": 166666,
    "totalPayment": 1833332,
    "installmentNumber": 3,
    "scheduledDisbursementDate": "2026-04-25",
    "status": "PAID"
  },
  "error": null
}

// 실패 응답
// 401 Unauthorized
{
  "success": false,
  "data": null,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "인증이 필요합니다."
  }
}
```

---

## 고용주 정산 상세 조회

`GET /api/v1/settlements/employer/{settlementId}`

---

용도: 특정 정산 회차의 상세 정보를 조회합니다. 본인 소유의 정산만 조회 가능합니다.

단일/페이징: 단일

JSON 형식:

```json
// 요청
// 없음 — Header: X-User-Id, Path: settlementId

// 성공 응답 (200 OK)
{
  "success": true,
  "data": {
    "id": 1,
    "contractId": 101,
    "projectName": "SaaS 대시보드 개발",
    "freelancerName": "이지연",
    "billingAmount": 1666666,
    "platformFee": 166666,
    "commissionRate": 0.10,
    "totalPayment": 1833332,
    "installmentNumber": 1,
    "status": "DISBURSED",
    "invoicePdfUrl": "https://s3.../invoice_1.pdf",
    "dueDate": "2026-03-25",
    "paidDate": "2026-02-27"
  },
  "error": null
}

// 실패 응답
// 403 Forbidden — 본인 정산이 아닌 경우
{
  "success": false,
  "data": null,
  "error": {
    "code": "ACCESS_DENIED",
    "message": "해당 정산에 접근할 권한이 없습니다."
  }
}
// 404 Not Found
{
  "success": false,
  "data": null,
  "error": {
    "code": "SETTLEMENT_NOT_FOUND",
    "message": "정산 내역을 찾을 수 없습니다."
  }
}
```

---

## 청구서 PDF 다운로드

`GET /api/v1/settlements/employer/{settlementId}/invoice`

---

용도: 특정 정산 회차의 청구서 PDF URL을 반환합니다. S3 pre-signed URL 형태로 반환됩니다.

단일/페이징: 단일

JSON 형식:

```json
// 요청
// 없음 — Header: X-User-Id, Path: settlementId

// 성공 응답 (200 OK)
{
  "success": true,
  "data": "https://s3.amazonaws.com/freebridge/invoices/invoice_1.pdf?X-Amz-Signature=...",
  "error": null
}

// 실패 응답
// 403 Forbidden
{
  "success": false,
  "data": null,
  "error": {
    "code": "ACCESS_DENIED",
    "message": "해당 정산에 접근할 권한이 없습니다."
  }
}
```

---

## 계약 선불 결제 검증 (PortOne)

`POST /api/v1/settlements/employer/verify-payment`

---

용도: 고용주가 프론트엔드에서 PortOne 결제를 완료한 후, imp_uid를 백엔드로 전달하여 결제를 검증합니다. 검증 성공 시 계약이 IN_PROGRESS로 활성화되고 정산 레코드가 생성됩니다. 동일 imp_uid로 재호출 시 기존 결과를 그대로 반환합니다(멱등성).

단일/페이징: 단일

JSON 형식:

```json
// 요청
{
  "imp_uid": "imp_1234567890",
  "contractId": 101
}

// 성공 응답 (200 OK)
{
  "success": true,
  "data": {
    "success": true,
    "contractId": 101,
    "totalVerifiedAmount": 10999992,
    "installmentsCreated": 6
  },
  "error": null
}

// 실패 응답
// 400 Bad Request — 결제 금액 불일치
{
  "success": false,
  "data": null,
  "error": {
    "code": "PAYMENT_AMOUNT_MISMATCH",
    "message": "결제 금액이 계약 금액과 일치하지 않습니다."
  }
}
// 400 Bad Request — PortOne 검증 실패
{
  "success": false,
  "data": null,
  "error": {
    "code": "PORTONE_VERIFICATION_FAILED",
    "message": "PortOne 결제 검증에 실패했습니다."
  }
}
```

---

# Freelancer Settlement

---

## 프리랜서 정산 목록 조회

`GET /api/v1/settlements/freelancer`

---

용도: 로그인한 프리랜서의 정산 목록을 조회합니다. 상태, 날짜 범위, 검색어, 정렬 기준으로 필터링 가능하며 페이지네이션을 지원합니다.

단일/페이징: 페이징

Query Parameters:
```
status    : "ALL" | "PENDING" | "PAID" | "CANCELLED"               (default: "ALL")
dateRange : "ALL" | "THIS_MONTH" | "LAST_MONTH" | "LAST_3_MONTHS"  (default: "ALL")
search    : string (프로젝트명 또는 고용주 이름)
sort      : "SCHEDULED_DATE_ASC" | "SCHEDULED_DATE_DESC" | "AMOUNT_DESC"  (default: "SCHEDULED_DATE_ASC")
page      : int  (default: 1)
size      : int  (default: 10)
```

JSON 형식:

```json
// 요청
// 없음 — Header: X-User-Id, Query Parameters로 전달

// 성공 응답 (200 OK)
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "contractId": 101,
        "employerSettlementId": 1,
        "projectName": "SaaS 대시보드 개발",
        "employerName": "테크스타트업",
        "totalAmount": 1666666,
        "platformFee": 166666,
        "tax": 49390,
        "netAmount": 1450610,
        "installmentNumber": 1,
        "status": "PAID",
        "scheduledDate": "2026-03-25",
        "paidDate": "2026-03-25",
        "receiptPdfUrl": "https://s3.../receipt_1.pdf"
      }
    ],
    "totalElements": 6,
    "totalPages": 1,
    "currentPage": 1
  },
  "error": null
}

// 실패 응답
// 401 Unauthorized
{
  "success": false,
  "data": null,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "인증이 필요합니다."
  }
}
```

---

## 프리랜서 정산 통계 조회

`GET /api/v1/settlements/freelancer/summary`

---

용도: 로그인한 프리랜서의 지급 예정 금액 및 지급 완료 금액 집계 데이터를 조회합니다.

단일/페이징: 단일

JSON 형식:

```json
// 요청
// 없음 — Header: X-User-Id

// 성공 응답 (200 OK)
{
  "success": true,
  "data": {
    "pendingAmount": 2901220,
    "pendingCount": 2,
    "paidAmount": 4351830,
    "paidCount": 3
  },
  "error": null
}

// 실패 응답
// 401 Unauthorized
{
  "success": false,
  "data": null,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "인증이 필요합니다."
  }
}
```

---

## 프리랜서 정산 상세 조회

`GET /api/v1/settlements/freelancer/{settlementId}`

---

용도: 특정 정산 회차의 상세 정보를 조회합니다. 본인 소유의 정산만 조회 가능합니다.

단일/페이징: 단일

JSON 형식:

```json
// 요청
// 없음 — Header: X-User-Id, Path: settlementId

// 성공 응답 (200 OK)
{
  "success": true,
  "data": {
    "id": 1,
    "contractId": 101,
    "employerSettlementId": 1,
    "projectName": "SaaS 대시보드 개발",
    "employerName": "테크스타트업",
    "paymentDay": 25,
    "totalAmount": 1666666,
    "platformFee": 166666,
    "commissionRate": 0.10,
    "tax": 49390,
    "netAmount": 1450610,
    "installmentNumber": 1,
    "status": "PAID",
    "scheduledDate": "2026-03-25",
    "paidDate": "2026-03-25",
    "receiptPdfUrl": "https://s3.../receipt_1.pdf"
  },
  "error": null
}

// 실패 응답
// 403 Forbidden
{
  "success": false,
  "data": null,
  "error": {
    "code": "ACCESS_DENIED",
    "message": "해당 정산에 접근할 권한이 없습니다."
  }
}
```

---

## 지급 영수증 PDF 다운로드

`GET /api/v1/settlements/freelancer/{settlementId}/receipt`

---

용도: PAID 상태인 정산 회차의 영수증 PDF URL을 반환합니다.

단일/페이징: 단일

JSON 형식:

```json
// 요청
// 없음 — Header: X-User-Id, Path: settlementId

// 성공 응답 (200 OK)
{
  "success": true,
  "data": "https://s3.amazonaws.com/freebridge/receipts/receipt_1.pdf?X-Amz-Signature=...",
  "error": null
}

// 실패 응답
// 400 Bad Request — PAID 상태가 아닌 정산
{
  "success": false,
  "data": null,
  "error": {
    "code": "INVALID_SETTLEMENT_STATUS",
    "message": "지급 완료된 정산만 영수증을 발급받을 수 있습니다."
  }
}
```

---

## 세금계산서 발행 요청

`POST /api/v1/settlements/freelancer/{settlementId}/tax-invoice`

---

용도: PAID 상태의 정산 회차에 대해 세금계산서 발행을 요청합니다. 이미 요청된 경우 409를 반환합니다.

단일/페이징: 단일

JSON 형식:

```json
// 요청
{
  "businessRegistrationNumber": "123-45-67890",
  "companyName": "이지연 1인사업자",
  "email": "jiyeon@example.com"
}

// 성공 응답 (200 OK)
{
  "success": true,
  "data": {
    "taxInvoiceId": 42,
    "settlementId": 1,
    "status": "REQUESTED",
    "requestedAt": "2026-02-27T10:30:00"
  },
  "error": null
}

// 실패 응답
// 409 Conflict — 이미 세금계산서 요청된 경우
{
  "success": false,
  "data": null,
  "error": {
    "code": "TAX_INVOICE_ALREADY_REQUESTED",
    "message": "이미 세금계산서 발행이 요청된 정산입니다."
  }
}
// 400 Bad Request — PAID 상태가 아닌 정산
{
  "success": false,
  "data": null,
  "error": {
    "code": "INVALID_SETTLEMENT_STATUS",
    "message": "지급 완료된 정산만 세금계산서를 요청할 수 있습니다."
  }
}
```

---

# Admin Settlement

---

## 정산 레코드 수동 생성

`POST /api/v1/settlements/generate`

---

용도: 특정 계약의 정산 레코드를 관리자가 수동으로 생성합니다. 정상 시나리오에서는 계약 서명 시 자동으로 처리되며, 이 API는 오류 복구용입니다.

단일/페이징: 단일

Query Parameters:
```
contractId : Long (필수)
```

JSON 형식:

```json
// 요청
// 없음 — Query Parameter: contractId
// 예시: POST /api/v1/settlements/generate?contractId=101

// 성공 응답 (200 OK)
{
  "success": true,
  "data": null,
  "error": null
}

// 실패 응답
// 404 Not Found — 존재하지 않는 contractId
{
  "success": false,
  "data": null,
  "error": {
    "code": "CONTRACT_NOT_FOUND",
    "message": "계약을 찾을 수 없습니다."
  }
}
// 400 Bad Request — 이미 정산 레코드가 존재하는 계약
{
  "success": false,
  "data": null,
  "error": {
    "code": "SETTLEMENT_ALREADY_EXISTS",
    "message": "이미 정산 레코드가 생성된 계약입니다."
  }
}
```

---

## 자동 지급 스케줄러 수동 실행

`POST /api/v1/settlements/disburse/run`

---

용도: scheduledDate가 오늘 이하인 PENDING 상태의 프리랜서 정산을 관리자가 즉시 수동으로 지급 처리합니다. 정상 시나리오에서는 매일 00:00 KST에 자동 실행됩니다.

단일/페이징: 단일

JSON 형식:

```json
// 요청
// 없음

// 성공 응답 (200 OK)
{
  "success": true,
  "data": null,
  "error": null
}

// 실패 응답
// 500 Internal Server Error — 지급 처리 중 오류
{
  "success": false,
  "data": null,
  "error": {
    "code": "DISBURSEMENT_FAILED",
    "message": "지급 처리 중 오류가 발생했습니다."
  }
}
```

---

## 전체 정산 목록 조회 (Admin)

`GET /api/v1/settlements/admin`

---

용도: 관리자가 모든 계약의 정산 레코드를 조회합니다. 상태 필터링 및 페이지네이션을 지원합니다.

단일/페이징: 페이징

Query Parameters:
```
status : "ALL" | "ISSUED" | "PAID" | "DISBURSED" | "CANCELLED"  (default: "ALL")
page   : int  (default: 1)
size   : int  (default: 10)
```

JSON 형식:

```json
// 요청
// 없음 — Query Parameters로 전달

// 성공 응답 (200 OK)
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "contractId": 101,
        "projectName": "SaaS 대시보드 개발",
        "freelancerName": "이지연",
        "billingAmount": 1666666,
        "platformFee": 166666,
        "totalPayment": 1833332,
        "installmentNumber": 1,
        "status": "DISBURSED",
        "invoicePdfUrl": "https://s3.../invoice_1.pdf",
        "dueDate": "2026-03-25",
        "paidDate": "2026-02-27"
      }
    ],
    "totalElements": 42,
    "totalPages": 5,
    "currentPage": 1
  },
  "error": null
}

// 실패 응답
// 403 Forbidden — 관리자 권한 없음
{
  "success": false,
  "data": null,
  "error": {
    "code": "ACCESS_DENIED",
    "message": "관리자만 접근할 수 있습니다."
  }
}
```

---

# Wallet

---

## 고용주 지갑 요약 조회

`GET /api/v1/wallets/employer/summary`

---

용도: 로그인한 고용주의 지갑 요약 정보(총 지출액, 거래 건수)를 조회합니다.

단일/페이징: 단일

JSON 형식:

```json
// 요청
// 없음 — Header: X-User-Id

// 성공 응답 (200 OK)
{
  "success": true,
  "data": {
    "totalPaidOut": 10999992,
    "transactionCount": 7
  },
  "error": null
}

// 실패 응답
// 401 Unauthorized
{
  "success": false,
  "data": null,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "인증이 필요합니다."
  }
}
```

---

## 고용주 거래 내역 조회

`GET /api/v1/wallets/employer/transactions`

---

용도: 로그인한 고용주의 거래 내역(계약 결제, 구독 결제, 환불 등)을 페이지네이션으로 조회합니다.

단일/페이징: 페이징

Query Parameters:
```
referenceType : "ALL" | "CONTRACT_PAYMENT" | "SUBSCRIPTION_PAYMENT" | "REFUND"  (default: "ALL")
page          : int  (default: 1)
size          : int  (default: 10)
```

JSON 형식:

```json
// 요청
// 없음 — Header: X-User-Id, Query Parameters로 전달

// 성공 응답 (200 OK)
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "type": "DEBIT",
        "amount": 10999992,
        "referenceType": "CONTRACT_PAYMENT",
        "referenceId": 101,
        "description": "계약 #101 선불 결제",
        "balanceAfter": 10999992,
        "createdAt": "2026-02-27T10:00:00"
      }
    ],
    "totalElements": 3,
    "totalPages": 1,
    "currentPage": 1
  },
  "error": null
}

// 실패 응답
// 401 Unauthorized
{
  "success": false,
  "data": null,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "인증이 필요합니다."
  }
}
```

---

## 프리랜서 지갑 요약 조회

`GET /api/v1/wallets/freelancer/summary`

---

용도: 로그인한 프리랜서의 지갑 요약 정보(총 수령액, 지급 예정 금액, 거래 건수)를 조회합니다.

단일/페이징: 단일

JSON 형식:

```json
// 요청
// 없음 — Header: X-User-Id

// 성공 응답 (200 OK)
{
  "success": true,
  "data": {
    "totalEarned": 4351830,
    "pendingAmount": 2901220,
    "transactionCount": 5
  },
  "error": null
}

// 실패 응답
// 401 Unauthorized
{
  "success": false,
  "data": null,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "인증이 필요합니다."
  }
}
```

---

## 프리랜서 거래 내역 조회

`GET /api/v1/wallets/freelancer/transactions`

---

용도: 로그인한 프리랜서의 지급 수령 내역을 페이지네이션으로 조회합니다.

단일/페이징: 페이징

Query Parameters:
```
page : int  (default: 1)
size : int  (default: 10)
```

JSON 형식:

```json
// 요청
// 없음 — Header: X-User-Id, Query Parameters로 전달

// 성공 응답 (200 OK)
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 5,
        "type": "CREDIT",
        "amount": 1450610,
        "referenceType": "FREELANCER_DISBURSEMENT",
        "referenceId": 1,
        "description": "계약 #101 1회차 정산 지급",
        "balanceAfter": 1450610,
        "createdAt": "2026-03-25T00:00:00"
      }
    ],
    "totalElements": 3,
    "totalPages": 1,
    "currentPage": 1
  },
  "error": null
}

// 실패 응답
// 401 Unauthorized
{
  "success": false,
  "data": null,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "인증이 필요합니다."
  }
}
```

---

## 플랫폼 에스크로 잔액 조회 (Admin)

`GET /api/v1/wallets/platform/escrow`

---

용도: 프리랜서에게 지급 예정으로 플랫폼이 보유 중인 에스크로 잔액을 조회합니다. PENDING 상태 FreelancerSettlement의 billingAmount 합산과 일치해야 합니다.

단일/페이징: 단일

JSON 형식:

```json
// 요청
// 없음

// 성공 응답 (200 OK)
{
  "success": true,
  "data": {
    "walletType": "PLATFORM_ESCROW",
    "balance": 5802440,
    "updatedAt": "2026-03-25T00:01:00"
  },
  "error": null
}

// 실패 응답
// 403 Forbidden
{
  "success": false,
  "data": null,
  "error": {
    "code": "ACCESS_DENIED",
    "message": "관리자만 접근할 수 있습니다."
  }
}
```

---

## 플랫폼 수익 잔액 조회 (Admin)

`GET /api/v1/wallets/platform/revenue`

---

용도: 플랫폼이 수수료 및 구독 결제로 획득한 수익 잔액을 조회합니다.

단일/페이징: 단일

JSON 형식:

```json
// 요청
// 없음

// 성공 응답 (200 OK)
{
  "success": true,
  "data": {
    "walletType": "PLATFORM_REVENUE",
    "balance": 1248762,
    "updatedAt": "2026-03-25T00:01:00"
  },
  "error": null
}

// 실패 응답
// 403 Forbidden
{
  "success": false,
  "data": null,
  "error": {
    "code": "ACCESS_DENIED",
    "message": "관리자만 접근할 수 있습니다."
  }
}
```

---

# Subscription Billing

---

## 구독 결제 내역 조회

`GET /api/v1/subscriptions/billing-history`

---

용도: 로그인한 고용주의 구독 업그레이드 결제 내역을 페이지네이션으로 조회합니다.

단일/페이징: 페이징

Query Parameters:
```
status : "ALL" | "PAID" | "FAILED"  (default: "ALL")
page   : int  (default: 1)
size   : int  (default: 10)
```

JSON 형식:

```json
// 요청
// 없음 — Header: X-User-Id, Query Parameters로 전달

// 성공 응답 (200 OK)
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 42,
        "planType": "PRIME",
        "amount": 99000,
        "status": "PAID",
        "billingDate": "2026-02-27",
        "paidDate": "2026-02-27"
      }
    ],
    "totalElements": 3,
    "totalPages": 1,
    "currentPage": 1
  },
  "error": null
}

// 실패 응답
// 401 Unauthorized
{
  "success": false,
  "data": null,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "인증이 필요합니다."
  }
}
```

---

# Internal Payment

---

## 구독 업그레이드 결제 처리 (Internal)

`POST /api/v1/internal/payments/subscription`

---

용도: 구독 모듈이 고용주의 플랜 업그레이드 시 호출하는 내부 API입니다. PortOne imp_uid를 검증하고 SubscriptionBilling을 생성하며, 성공 시 PLATFORM_REVENUE에 크레딧합니다. 동일 imp_uid 재호출 시 기존 결과를 반환합니다(멱등성).

단일/페이징: 단일

JSON 형식:

```json
// 요청
{
  "employerId": 7,
  "planType": "PRIME",
  "amount": 99000,
  "imp_uid": "imp_1234567890"
}

// 성공 응답 (200 OK)
{
  "success": true,
  "data": {
    "success": true,
    "billingId": 42,
    "employerId": 7,
    "planType": "PRIME",
    "amount": 99000,
    "status": "PAID",
    "paidDate": "2026-02-27",
    "errorCode": null,
    "message": null
  },
  "error": null
}

// 실패 응답
// 200 OK (결제 실패는 HTTP 200으로 반환, success 필드로 구분)
{
  "success": true,
  "data": {
    "success": false,
    "billingId": 43,
    "employerId": 7,
    "planType": "PRIME",
    "amount": 99000,
    "status": "FAILED",
    "paidDate": null,
    "errorCode": "PORTONE_VERIFICATION_FAILED",
    "message": "결제 금액(39000)이 플랜 가격(99000)과 일치하지 않습니다."
  },
  "error": null
}
```

---

## 구독 결제 내역 단건 조회 (Internal)

`GET /api/v1/internal/payments/subscription/{billingId}`

---

용도: 구독 모듈이 billingId로 특정 SubscriptionBilling 레코드를 조회하는 내부 API입니다.

단일/페이징: 단일

JSON 형식:

```json
// 요청
// 없음 — Path: billingId

// 성공 응답 (200 OK)
{
  "success": true,
  "data": {
    "id": 42,
    "planType": "PRIME",
    "amount": 99000,
    "status": "PAID",
    "billingDate": "2026-02-27",
    "paidDate": "2026-02-27"
  },
  "error": null
}

// 실패 응답
// 404 Not Found
{
  "success": false,
  "data": null,
  "error": {
    "code": "BILLING_NOT_FOUND",
    "message": "결제 내역을 찾을 수 없습니다."
  }
}
```

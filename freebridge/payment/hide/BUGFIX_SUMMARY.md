# Payment Module — Bug Fix Summary

> Branch: `HFP-80-feat-subscription`
> Fix worktree: `hfp80-fix`
> 수정 일자: 2026-03-04

---

## Fix 1 — `SubscriptionPaymentService.processPayment()` / `chargeScheduled()`: 중복 상태 설정 제거

**파일:** `service/SubscriptionPaymentService.java`

**버그:**
`processPayment()`에서 `markPaid()`를 호출하기 직전에 `setTransactionId()`와 `setStatus(PAID)`를 이미 호출하고 있었음.
`markPaid(String transactionId)`는 내부적으로 `status = PAID`, `paidDate = today`, `transactionId = transactionId`를 한꺼번에 처리하기 때문에 앞의 두 setter는 즉시 덮어써지는 중복이었음.
`chargeScheduled()`에서도 동일하게 `setStatus(PAID)` 후 `markPaid()`를 호출하는 중복이 있었음.

**수정:**
- `processPayment()`: `billing.setTransactionId(...)` + `billing.setStatus(PAID)` 두 줄 제거. `billing.setBillingDate(LocalDate.now())` 는 `markPaid()`가 처리하지 않으므로 유지.
- `chargeScheduled()`: `billing.setStatus(PAID)` 한 줄 제거.

```java
// Before
billing.setTransactionId(paymentInfo.getPaymentId());
billing.setStatus(SubscriptionBillingStatus.PAID);
billing.setBillingDate(LocalDate.now());
billing.markPaid(paymentInfo.getPaymentId());

// After
billing.setBillingDate(LocalDate.now());
billing.markPaid(paymentInfo.getPaymentId()); // status=PAID, paidDate, transactionId 일괄 설정
```

---

## Fix 2 — `VerifyPaymentRequest`: 검증 어노테이션 누락

**파일:** `api/web/dto/VerifyPaymentRequest.java`

**버그:**
컨트롤러에서 `@Valid`를 사용했지만 DTO에 `@NotBlank` / `@NotNull`이 없어서 `paymentId = null`이나 `contractId = null`로 요청해도 서비스까지 그대로 통과했음.

**수정:**
```java
// Before
private String paymentId;
private Long contractId;

// After
@NotBlank(message = "paymentId는 필수입니다.")
private String paymentId;

@NotNull(message = "contractId는 필수입니다.")
private Long contractId;
```

---

## Fix 3 — `EmployerSettlementService.verifyContractPayment()`: 고용주 지갑 잔고 미차감

**파일:** `service/EmployerSettlementService.java`

**버그:**
계약금 결제 검증 성공 시 고용주 지갑에 WalletTransaction(DEBIT) 레코드만 저장하고, 실제 `wallet.balance`를 차감하지 않았음.
결과: 고용주 지갑 잔고는 항상 0 (또는 초기값), `balanceAfter` 스냅샷도 잘못된 값으로 기록됨.

**수정:**
```java
// Before
Wallet employerWallet = getOrCreateUserWallet(employerId, WalletType.EMPLOYER);
walletTransactionRepository.save(new WalletTransaction(
        employerWallet.getId(), TransactionType.DEBIT, totalExpected, ...
        "계약금 결제 (계약 #" + contractId + ")", employerWallet.getBalance()));

// After
Wallet employerWallet = getOrCreateUserWallet(employerId, WalletType.EMPLOYER);
employerWallet.debit(totalExpected);          // 잔고 차감
walletRepository.save(employerWallet);        // 저장
walletTransactionRepository.save(new WalletTransaction(
        employerWallet.getId(), TransactionType.DEBIT, totalExpected, ...
        "계약금 결제 (계약 #" + contractId + ")", employerWallet.getBalance()));
```

---

## Fix 4 — `EmployerSettlementService.cancelAndRefund()`: escrow debit 불일치 + 고용주 credit 누락

**파일:** `service/EmployerSettlementService.java`

**버그 (2개):**

1. `escrowWallet.setBalance(escrowWallet.getBalance() - refundAmount)` — `Wallet.debit()` 메서드 대신 `setBalance()`를 직접 조작해서 코드 일관성 위반. `debit()`은 내부적으로 다른 유효성 로직이 추가될 수 있으므로 직접 조작은 위험.

2. 고용주 지갑 `credit()` 미호출 — WalletTransaction(CREDIT) 레코드만 저장하고 실제 `wallet.balance`를 증가시키지 않았음. 환불됐음에도 고용주 지갑 잔고는 변하지 않음.

**수정:**
```java
// Before
Wallet escrowWallet = getOrCreatePlatformWallet(WalletType.PLATFORM_ESCROW);
escrowWallet.setBalance(escrowWallet.getBalance() - refundAmount);  // ❌
walletRepository.save(escrowWallet);

Wallet employerWallet = getOrCreateUserWallet(employerId, WalletType.EMPLOYER);
// credit() 미호출 ❌
walletTransactionRepository.save(new WalletTransaction(...));

// After
Wallet escrowWallet = getOrCreatePlatformWallet(WalletType.PLATFORM_ESCROW);
escrowWallet.debit(refundAmount);                                   // ✅
walletRepository.save(escrowWallet);

Wallet employerWallet = getOrCreateUserWallet(employerId, WalletType.EMPLOYER);
employerWallet.credit(refundAmount);                                // ✅
walletRepository.save(employerWallet);
walletTransactionRepository.save(new WalletTransaction(...));
```

---

## Fix 5 — `AdminSettlementService.cancelContractSettlements()`: PortOne 취소 API 미호출 + 고용주 환불 누락

**파일:** `service/AdminSettlementService.java`

**버그 (2개):**

1. DB 레코드를 CANCELLED로 바꾸고 escrow 잔고를 차감했지만, **PortOne에 실제 환불을 요청하지 않았음**. 고용주의 결제 수단(카드 등)으로 환불이 이루어지지 않아 고객에게 돈이 돌아가지 않는 심각한 버그.

2. 고용주 지갑에 환불 금액을 `credit`하지 않음 + WalletTransaction 기록도 없음.

**수정:**
- `PortOneApiClient` 필드 추가 (`@RequiredArgsConstructor` 자동 DI).
- 루프 전에 `paymentId`, `employerId` 추출 (모든 PAID 회차는 동일 transactionId 공유).
- 에스크로 처리 완료 후: `portOneApiClient.cancelPayment(paymentId, refundTotal, "관리자 계약 취소 환불")` 호출.
- 고용주 지갑 `credit(refundTotal)` + `walletRepository.save()` + `WalletTransaction(CREDIT, REFUND)` 기록 추가.

```java
// 추가된 핵심 코드
portOneApiClient.cancelPayment(paymentId, refundTotal, "관리자 계약 취소 환불");

Wallet employerWallet = employerSettlementService.getOrCreateUserWallet(employerId, WalletType.EMPLOYER);
employerWallet.credit(refundTotal);
walletRepository.save(employerWallet);
walletTransactionRepository.save(new WalletTransaction(
        employerWallet.getId(), TransactionType.CREDIT, refundTotal,
        TransactionReferenceType.REFUND, contractId,
        "계약 취소 환불 입금 (계약 #" + contractId + ")",
        employerWallet.getBalance()));
```

---

## Fix 6 — `WalletService.getFreelancerSummary()`: pendingAmount 항상 0L

**파일:** `service/WalletService.java`

**버그:**
`FreelancerWalletSummaryResponse`의 두 번째 인자 `pendingAmount`가 `0L`로 하드코딩됨. 프리랜서가 지갑 요약을 조회하면 지급 예정 금액이 항상 0으로 표시됨.

**수정:**
- `FreelancerSettlementRepository` 필드 주입 추가.
- `sumNetAmountByFreelancerIdAndStatusPending(freelancerId)` 쿼리로 PENDING 상태 정산의 실제 netAmount 합산.
- 지갑이 아직 생성되지 않은 프리랜서도 pendingAmount 조회가 가능하도록 지갑 null 체크 전에 먼저 계산.

```java
// Before
Long totalEarned = wallet.getBalance();
Integer transactionCount = ...;
return new FreelancerWalletSummaryResponse(totalEarned, 0L, transactionCount); // ❌

// After
Long pendingAmount = freelancerSettlementRepository
        .sumNetAmountByFreelancerIdAndStatusPending(freelancerId); // ✅

if (wallet == null) {
    return new FreelancerWalletSummaryResponse(0L, pendingAmount, 0);
}
Long totalEarned = wallet.getBalance();
Integer transactionCount = ...;
return new FreelancerWalletSummaryResponse(totalEarned, pendingAmount, transactionCount);
```

---

## Fix 7 — `AdminSettlementController` / `WalletController`: Admin 권한 체크 없음

**파일:** `api/web/AdminSettlementController.java`, `api/web/WalletController.java`

**버그:**
`[Admin]` 주석이 붙은 엔드포인트들에 실제 Spring Security 권한 체크가 없어 인증된 일반 사용자도 호출 가능했음.

**수정:**
- `AdminSettlementController` 클래스 레벨에 `@PreAuthorize("hasRole('ADMIN')")` 추가.
  → `/generate`, `/disburse/run`, `/cancel`, `/admin` 4개 엔드포인트 모두 보호.
- `WalletController`의 `escrowBalance()`, `revenueBalance()` 메서드에 각각 `@PreAuthorize("hasRole('ADMIN')")` 추가.

```java
// AdminSettlementController
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettlementController { ... }

// WalletController (개별 메서드)
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/platform/escrow")
public ResponseEntity<...> escrowBalance() { ... }
```

---

## Fix 8 — `InternalPaymentController.processSubscriptionPayment()`: employerId 클라이언트 위조 가능

**파일:** `api/web/InternalPaymentController.java`

**버그:**
`employerId`를 `@RequestBody SubscriptionPaymentRequest` 바디에서 그대로 신뢰했음. 악의적인 클라이언트가 다른 고용주의 ID를 요청 바디에 실어 그 고용주 명의로 결제를 유발할 수 있었음.

**수정:**
- `@AuthenticationPrincipal CustomUserDetails user` 파라미터 추가.
- `user.getId()`로 새 `SubscriptionPaymentRequest`를 생성해서 처리. 요청 바디의 `employerId`는 완전히 무시됨.

```java
// Before
public ResponseEntity<...> processSubscriptionPayment(
        @RequestBody SubscriptionPaymentRequest request) {
    SubscriptionPaymentResponse response = subscriptionPaymentService.processPayment(request);
    ...
}

// After
public ResponseEntity<...> processSubscriptionPayment(
        @AuthenticationPrincipal CustomUserDetails user,
        @RequestBody SubscriptionPaymentRequest request) {
    // employerId는 토큰에서 추출 (클라이언트 바디 위조 방지)
    SubscriptionPaymentRequest secureRequest = new SubscriptionPaymentRequest(
            user.getId(), request.getPlanType(), request.getAmount(), request.getBillingKey());
    SubscriptionPaymentResponse response = subscriptionPaymentService.processPayment(secureRequest);
    ...
}
```

---

## 수정된 파일 목록

| 파일 | 수정 내용 |
|------|---------|
| `service/SubscriptionPaymentService.java` | Fix 1: 중복 setter 제거 |
| `api/web/dto/VerifyPaymentRequest.java` | Fix 2: @NotBlank / @NotNull 추가 |
| `service/EmployerSettlementService.java` | Fix 3: employer debit 추가 / Fix 4: escrow debit 방식 변경 + employer credit 추가 |
| `service/AdminSettlementService.java` | Fix 5: PortOneApiClient 주입, cancelPayment 호출, employer credit 추가 |
| `service/WalletService.java` | Fix 6: FreelancerSettlementRepository 주입, pendingAmount 실제 계산 |
| `api/web/AdminSettlementController.java` | Fix 7: 클래스 레벨 @PreAuthorize 추가 |
| `api/web/WalletController.java` | Fix 7: 플랫폼 지갑 엔드포인트 @PreAuthorize 추가 |
| `api/web/InternalPaymentController.java` | Fix 8: @AuthenticationPrincipal 추가, employerId 토큰에서 추출 |

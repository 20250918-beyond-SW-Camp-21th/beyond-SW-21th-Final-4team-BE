# Payment Module — Service & Function Reference

Detailed documentation for every service class and every public method in the payment module.

---

## Table of Contents

1. [WalletService](#1-walletservice)
2. [EmployerSettlementService](#2-employersettlementservice)
3. [FreelancerSettlementService](#3-freelancersettlementservice)
4. [AdminSettlementService](#4-adminsettlementservice)
5. [SubscriptionPaymentService](#5-subscriptionpaymentservice)
6. [SubscriptionBillingService](#6-subscriptionbillingservice)
7. [BillingKeyService](#7-billingkeyservice)
8. [PortOneApiClient](#8-portoneapiclient)
9. [SettlementScheduler](#9-settlementscheduler)

---

## 1. WalletService

**Purpose:** Read-only queries for wallet balances and transaction history. Never writes to the database — all wallet mutations happen inside `EmployerSettlementService` and `AdminSettlementService`.

**Dependencies:** `WalletRepository`, `WalletTransactionRepository`, `FreelancerSettlementRepository`

---

### `getEmployerSummary(Long employerId) → EmployerWalletSummaryResponse`

Returns aggregate statistics for an employer's virtual wallet.

- Looks up the `EMPLOYER` wallet by `ownerId`.
- If no wallet exists yet (employer has never paid), returns zeros.
- Queries the sum of all DEBIT transactions (`sumDebitByWalletId`) as `totalPaidOut`.
- Queries the total transaction count (`countByWalletId`).

**Note:** The employer wallet is virtual — its `balance` field is not meaningful. Only debit records exist (contract payments). Use `totalPaidOut` as the real figure.

**Returns:** `{ totalPaidOut: Long, transactionCount: int }`

---

### `getEmployerTransactions(Long employerId, String referenceType, int page, int size) → PageResponse<WalletTransactionItem>`

Returns paginated transaction history for an employer's wallet, sorted newest first.

- `referenceType = "ALL"` returns all transactions.
- Any other value (e.g. `"CONTRACT_PAYMENT"`) filters by `TransactionReferenceType` enum.
- `page` is 1-based externally; converted to 0-based for JPA internally.
- Returns an empty page (not an error) if the wallet does not exist.

**Returns:** Paginated list of `WalletTransactionItem` (id, type, amount, referenceType, referenceId, description, balanceAfter, createdAt)

---

### `getFreelancerSummary(Long freelancerId) → FreelancerWalletSummaryResponse`

Returns aggregate statistics for a freelancer's wallet.

- Looks up the `FREELANCER` wallet by `ownerId`.
- If no wallet exists yet, returns zeros.
- `totalEarned` = current wallet `balance` (cumulative net payments received).
- `pendingAmount` = `FreelancerSettlementRepository.sumNetAmountByFreelancerIdAndStatusPending(freelancerId)` — PENDING 상태 정산의 netAmount 합산. 지갑이 아직 생성되지 않은 프리랜서도 조회 가능.
- `transactionCount` = total number of credit transactions.

**Returns:** `{ totalEarned: Long, pendingAmount: Long, transactionCount: int }`

---

### `getFreelancerTransactions(Long freelancerId, int page, int size) → PageResponse<WalletTransactionItem>`

Returns all paginated transaction history for a freelancer's wallet, sorted newest first.

- No `referenceType` filter — freelancers always see all transactions.
- Returns an empty page if the wallet does not exist.

---

### `getEscrowWallet() → PlatformWalletResponse`

Returns the current balance of the `PLATFORM_ESCROW` singleton wallet.

- If the escrow wallet has never been created (no payments processed), returns a synthetic response with `balance = 0`.
- Escrow holds employer contract payments until disbursement day.

**Returns:** `{ walletType: "PLATFORM_ESCROW", balance: Long, updatedAt: LocalDateTime }`

---

### `getRevenueWallet() → PlatformWalletResponse`

Returns the current balance of the `PLATFORM_REVENUE` singleton wallet.

- If no revenue has ever been collected, returns a synthetic response with `balance = 0`.
- Revenue accumulates platform fees (from disbursement) and subscription payments.

**Returns:** `{ walletType: "PLATFORM_REVENUE", balance: Long, updatedAt: LocalDateTime }`

---

## 2. EmployerSettlementService

**Purpose:** Core payment processing service. Handles PortOne payment verification, settlement record creation (both employer and freelancer sides), and escrow wallet crediting. Also provides read-only views for the employer portal.

**Dependencies:** `EmployerSettlementRepository`, `FreelancerSettlementRepository`, `WalletRepository`, `WalletTransactionRepository`, `ContractQuery`, `PortOneApiClient`

---

### `listSettlements(Long employerId, String status, String dateRange, String search, String sort, int page, int size) → PageResponse<EmployerSettlementItem>`

Returns a paginated, filtered, sorted list of an employer's settlement records.

**Filter priority (only one applies at a time):**
1. If `status != "ALL"` → filter by `EmployerSettlementStatus` (ISSUED, PAID, DISBURSED, CANCELLED)
2. Else if `dateRange != "ALL"` → filter by `dueDate` range (LAST_3_MONTHS / LAST_6_MONTHS / LAST_1_YEAR)
3. Else → return all settlements for this employer

**Sort options:** `DUE_DATE_ASC` (default), `DUE_DATE_DESC`, `AMOUNT_ASC`, `AMOUNT_DESC`

**Note:** `projectName` and `freelancerName` fields in the returned items are currently `null` (populated only in the detail endpoint where a `ContractQuery` call is made).

---

### `getSummary(Long employerId) → EmployerSettlementSummaryResponse`

Returns aggregate financial statistics across all of an employer's settlements.

- `totalPaidAmount` — sum of `totalPayment` for all PAID records
- `totalDisbursedAmount` — sum of `totalPayment` for all DISBURSED records
- `paidCount`, `disbursedCount`, `cancelledCount` — counts per status

All aggregate queries return `null` from the repository if there are no matching records; callers should treat `null` as `0`.

---

### `getNextSettlement(Long employerId) → EmployerSettlementNextResponse`

Returns the single next upcoming PAID settlement for the employer (the one with the earliest `dueDate` that is still in PAID status, meaning funds are held in escrow and disbursement has not yet occurred).

- Uses `findFirstPaidByEmployerId` (sorted by `dueDate ASC`, limit 1).
- Returns `null` if no upcoming settlement exists.

---

### `getSettlementDetail(Long employerId, Long settlementId) → EmployerSettlementDetailResponse`

Returns full detail for a single settlement, including `commissionRate` and `projectName` fetched from the contract module.

- Throws `SETTLEMENT_NOT_FOUND` if the record does not exist.
- Throws `SETTLEMENT_FORBIDDEN` if `settlement.employerId != employerId` (ownership check).
- Calls `ContractQuery.getContractInfo()` to get `projectName` and `commissionRate`.

---

### `getInvoicePdfUrl(Long employerId, Long settlementId) → String`

Returns the S3 URL of the invoice PDF for a specific settlement.

- Throws `SETTLEMENT_NOT_FOUND` / `SETTLEMENT_FORBIDDEN` on invalid access.
- Returns `null` if the PDF has not been generated yet (field is nullable).

---

### `verifyContractPayment(String paymentId, Long contractId, Long employerId) → VerifyPaymentResponse`

**The main payment entry point.** Verifies a PortOne payment and generates all settlement records for the contract.

**Steps:**

1. **Idempotency check** — if `transactionId = paymentId` already exists in `EmployerSettlement`, returns the cached result immediately without re-processing. Safe to call multiple times with the same `paymentId`.

2. **PortOne verification** — calls `PortOneApiClient.getPayment(paymentId)`. Throws `PAYMENT_FAILED` if status is not `"PAID"`.

3. **Contract lookup** — calls `ContractQuery.getContractInfo(contractId)` to get budget, commission rate, payment day, start/end dates.

4. **Settlement record creation** — delegates to `createSettlementRecords()`.

5. **Amount validation** — compares the sum of all `EmployerSettlement.totalPayment` values against `PortOnePaymentInfo.totalAmount`. Throws `PAYMENT_AMOUNT_MISMATCH` if they differ (logs a warning).

6. **Escrow crediting** — credits `PLATFORM_ESCROW` wallet by the total verified amount. Creates the wallet if it does not exist.

7. **Employer wallet debit** — calls `employerWallet.debit(totalExpected)` and saves before creating the WalletTransaction record, so that the `balanceAfter` snapshot is accurate.

8. **Transaction recording** — saves two `WalletTransaction` records: a DEBIT on the employer's virtual wallet and a CREDIT on the escrow wallet.

**Returns:** `{ success: true, contractId, totalVerifiedAmount, installmentsCreated }`

---

### `createSettlementRecords(ContractInfo contract, String paymentId, Long employerId) → List<EmployerSettlement>`

**Shared utility used by both `verifyContractPayment` and `AdminSettlementService.generateSettlements`.**

Creates one `EmployerSettlement` + one `FreelancerSettlement` pair for each installment month of the contract.

**Installment calculation:**
```
totalMonths     = months between startDate and endDate (inclusive), minimum 1
baseInstallment = budget / totalMonths  (integer division)

For installment i (1-indexed):
  billingAmount  = baseInstallment                          (months 1..N-1)
                 = budget - baseInstallment*(totalMonths-1) (last month absorbs rounding remainder)
  platformFee    = floor(billingAmount * commissionRate)    (employer-side fee)
  totalPayment   = billingAmount + platformFee
  dueDate        = startDate + (i-1) months, day clamped to paymentDay or end-of-month

  fsPlatformFee  = floor(billingAmount * commissionRate)    (freelancer-side deduction)
  tax            = floor((billingAmount - fsPlatformFee) * 0.033)
  netAmount      = billingAmount - fsPlatformFee - tax      (what freelancer receives)
```

Each `EmployerSettlement` is created with status `PAID` and `paidDate = today`.
Each `FreelancerSettlement` is created with status `PENDING` and `scheduledDate = dueDate`.

---

## 3. FreelancerSettlementService

**Purpose:** Read-only queries for the freelancer's settlement portal, plus the tax invoice request stub.

**Dependencies:** `FreelancerSettlementRepository`, `ContractQuery`

---

### `listSettlements(Long freelancerId, String status, String dateRange, String search, String sort, int page, int size) → PageResponse<FreelancerSettlementItem>`

Returns a paginated, filtered, sorted list of a freelancer's settlement records.

- If `status != "ALL"` → filters by `FreelancerSettlementStatus` (PENDING, PAID, CANCELLED).
- `dateRange` filtering is declared in the signature but **not yet implemented** in the current code — the `dateRange` parameter is accepted but ignored; only `status` filtering is applied.
- **Sort options:** `SCHEDULED_DATE_ASC` (default), `SCHEDULED_DATE_DESC`, `AMOUNT_ASC` (by `netAmount`), `AMOUNT_DESC`
- `projectName` and `employerName` in the returned items are currently `null`.

---

### `getSummary(Long freelancerId) → FreelancerSettlementSummaryResponse`

Returns aggregate financial statistics for a freelancer.

- `pendingAmount` — sum of `netAmount` for all PENDING records (money owed but not yet paid out)
- `paidAmount` — sum of `netAmount` for all PAID records (money already received)
- `pendingCount`, `paidCount` — counts per status

---

### `getSettlementDetail(Long freelancerId, Long settlementId) → FreelancerSettlementDetailResponse`

Returns full detail for a single freelancer settlement record.

- Throws `SETTLEMENT_NOT_FOUND` / `SETTLEMENT_FORBIDDEN` on invalid access.
- Calls `ContractQuery.getContractInfo()` to populate `projectName`, `paymentDay`, and `commissionRate`.
- `employerName` is currently `null` (not fetched from any module).

---

### `getReceiptPdfUrl(Long freelancerId, Long settlementId) → String`

Returns the S3 URL of the receipt PDF.

- Throws `SETTLEMENT_NOT_FOUND` / `SETTLEMENT_FORBIDDEN` on invalid access.
- Returns `null` if the PDF has not been generated yet.

---

### `requestTaxInvoice(Long freelancerId, Long settlementId, TaxInvoiceRequest request) → TaxInvoiceResponse`

Registers a tax invoice request for a PAID settlement.

- Throws `SETTLEMENT_FORBIDDEN` on ownership mismatch.
- Throws `INVALID_INPUT_VALUE` if the settlement status is not `PAID` (tax invoice can only be requested after payment).
- **Current implementation is a stub** — no external tax system is integrated. Returns `{ taxInvoiceId: null, settlementId, status: "REQUESTED", requestedAt: now }`.

---

## 4. AdminSettlementService

**Purpose:** Admin and scheduler operations — manually generating settlement records, running the disbursement process, and cancelling contracts with PortOne refund.

**Dependencies:** `EmployerSettlementRepository`, `FreelancerSettlementRepository`, `WalletRepository`, `WalletTransactionRepository`, `ContractQuery`, `EmployerSettlementService`, `PortOneApiClient`

---

### `generateSettlements(Long contractId)`

Manually creates settlement records for a contract without going through PortOne payment verification. Used by admins and for testing.

- Fetches contract info via `ContractQuery`.
- Checks if `EmployerSettlement` records already exist for this `contractId`. If they do, **logs a warning and returns without creating anything** (safe to call multiple times).
- Delegates to `EmployerSettlementService.createSettlementRecords()` using `paymentId = "MANUAL-{contractId}"`.

**Important:** Records created via this method are marked as PAID immediately (same as verified payments). This is intended for admin/test use only.

---

### `runDisbursement()`

**The disbursement engine.** Processes all freelancer settlements that are due today or overdue.

**Query:** `FreelancerSettlement` where `status = PENDING` AND `scheduledDate <= today`.

For each matching record, calls `processSingleDisbursement()`. Errors on individual records are caught, logged, and skipped — the loop continues with the next record.

Logs a summary at the end: `"정산 실행 완료: 성공=X/Y"`.

---

### `processSingleDisbursement(FreelancerSettlement fs, Wallet escrowWallet, Wallet revenueWallet)` *(private)*

Executes the money movement for a single freelancer settlement:

1. **Loads** the paired `EmployerSettlement` via `fs.employerSettlementId`. Throws `SETTLEMENT_NOT_FOUND` if missing.

2. **Debits PLATFORM_ESCROW** by `EmployerSettlement.totalPayment` (billingAmount + platformFee).

3. **Credits PLATFORM_REVENUE** by `EmployerSettlement.platformFee + FreelancerSettlement.tax` (the platform keeps both the employer-side commission and the freelancer-side income tax withholding).

4. **Credits FREELANCER wallet** by `FreelancerSettlement.netAmount` (creates the wallet if needed).

5. **Records 3 WalletTransaction entries:**
   - DEBIT on ESCROW (`referenceType = FREELANCER_DISBURSEMENT`)
   - CREDIT on REVENUE (`referenceType = PLATFORM_FEE`)
   - CREDIT on freelancer wallet (`referenceType = FREELANCER_DISBURSEMENT`)

6. **Marks** `EmployerSettlement → DISBURSED` and `FreelancerSettlement → PAID`.

---

### `cancelContractSettlements(Long contractId) → CancellationResult`

관리자 전용 계약 취소 처리. 에스크로에 묶인 `PAID` 상태 회차만 처리하며, `DISBURSED` 회차는 이미 프리랜서에게 지급 완료이므로 제외.

**Steps:**

1. `contractId`로 모든 `EmployerSettlement` 조회 → `status = PAID`인 것만 필터링. 없으면 즉시 반환.

2. 취소 대상의 첫 번째 레코드에서 `paymentId`(`transactionId`)와 `employerId` 추출 (모든 PAID 회차는 동일 PortOne paymentId 공유).

3. 루프: 각 `EmployerSettlement`에 대해:
   - `escrowWallet.debit(es.getTotalPayment())` + WalletTransaction(DEBIT, REFUND) 기록.
   - `es.cancel()` + `fs.cancel()` 상태 변경.
   - `refundTotal` 누산.

4. `walletRepository.save(escrowWallet)` — 에스크로 잔고 저장.

5. **`portOneApiClient.cancelPayment(paymentId, refundTotal, "관리자 계약 취소 환불")`** — PortOne에 실제 환불 요청. 실패 시 예외 발생.

6. 고용주 지갑 `credit(refundTotal)` + `walletRepository.save()` + WalletTransaction(CREDIT, REFUND) 기록.

**Returns:** `CancellationResult(contractId, cancelledInstallments, refundedAmount)`

---

### `listAllSettlements(String status, int page, int size) → PageResponse<EmployerSettlementItem>`

Admin view of all settlements across all contracts, sorted newest first by `createdAt`.

- Optionally filters by `EmployerSettlementStatus`.
- Uses `findAll(pageable)` when `status = "ALL"`.

---

## 5. SubscriptionPaymentService

**Purpose:** Subscription payment processing — both on-demand (called by other modules or the internal API) and scheduled (called by the monthly cron). Also implements the `SubscriptionPaymentQuery` interface for cross-module access.

**Dependencies:** `SubscriptionBillingRepository`, `BillingKeyRepository`, `WalletRepository`, `WalletTransactionRepository`, `PortOneApiClient`

**Implements:** `SubscriptionPaymentQuery` (`com.fallguys.payment.api.shared`)

---

### `processPayment(SubscriptionPaymentRequest request) → SubscriptionPaymentResponse`

Processes a new subscription payment. Called on plan purchase or upgrade.

**Steps:**

1. Validates `planType` string against `PlanType` enum. Throws `INVALID_INPUT_VALUE` on invalid value.

2. **Calls PortOne** via `chargeBillingKey(billingKey, amount, orderName, customerId)`.
   - On `BusinessException` (PortOne error): saves a `SubscriptionBilling(FAILED)` record and returns a failure response immediately.
   - If PortOne responds but `status != "PAID"`: same failure path.

3. **Rotates the billing key:**
   - Deactivates any existing active `BillingKey` for this employer (calls `deactivate()` + saves).
   - Saves a new `BillingKey(employerId, billingKey, planType)` with `active = true`.

4. **Creates `SubscriptionBilling` record** — `setBillingDate(today)` 후 `markPaid(paymentId)` 단 한 번 호출. `markPaid()`가 `status = PAID`, `paidDate = today`, `transactionId`를 한꺼번에 처리하므로 별도 setter를 호출하지 않음.

5. **Credits `PLATFORM_REVENUE` wallet** by `amount`. Creates wallet if it does not exist.

6. **Records one `WalletTransaction`** (CREDIT, `referenceType = SUBSCRIPTION_PAYMENT`).

**Returns:** `SubscriptionPaymentResponse` — check `success` field to determine outcome.

---

### `chargeScheduled(BillingKey billingKey, long amount)`

**Scheduler-only auto-charge method.** Charges an existing billing key for monthly subscription renewal.

Key differences from `processPayment`:
- Does **not** rotate the `BillingKey` entity — the existing key stays active unchanged.
- Does **not** deactivate old keys or save new ones.
- Called by `SettlementScheduler.runMonthlySubscriptionBilling()` for each active `BillingKey`.

**Steps:**
1. Creates a new `SubscriptionBilling` record (status determined by outcome).
2. Calls `PortOneApiClient.chargeBillingKey()`.
3. On success: marks billing `PAID`, credits PLATFORM_REVENUE, records transaction.
4. On PortOne failure or non-PAID status: marks billing `FAILED`, logs warning.
5. On `BusinessException`: marks billing `FAILED`, logs error, **re-throws** so the scheduler can log individual failures.

---

### `getBillingById(Long billingId) → SubscriptionBillingItem`

Returns a single subscription billing record by ID.

- Throws `SETTLEMENT_NOT_FOUND` if the record does not exist (reuses the settlement error code).

---

### `processSubscriptionPayment(Long employerId, String planType, long amount, String billingKey) → SubscriptionPaymentResult`

**Implements `SubscriptionPaymentQuery` interface.** Called by other modules (e.g. `subscription`) to trigger a subscription payment in-process without going through HTTP.

- Wraps the parameters into a `SubscriptionPaymentRequest` and delegates to `processPayment()`.
- Maps the `SubscriptionPaymentResponse` to a `SubscriptionPaymentResult` (same data, different type for module boundary).

---

## 6. SubscriptionBillingService

**Purpose:** Read-only query for subscription billing history. Thin service — just a paged query over `SubscriptionBilling` records.

**Dependencies:** `SubscriptionBillingRepository`

---

### `getBillingHistory(Long employerId, String status, int page, int size) → PageResponse<SubscriptionBillingItem>`

Returns paginated subscription payment history for an employer.

- `status = "ALL"` returns all records.
- Any other value filters by `SubscriptionBillingStatus` (PENDING, PAID, FAILED).
- Sorted by `billingDate DESC` (newest first).
- `page` is 1-based externally.

**Returns:** Paginated list of `SubscriptionBillingItem` (id, planType, amount, status, billingDate, paidDate)

---

## 7. BillingKeyService

**Purpose:** Utility service for managing `BillingKey` entities. Used internally and could be used by other modules that need to inspect billing key state.

**Dependencies:** `BillingKeyRepository`

---

### `getActiveBillingKey(Long employerId) → Optional<BillingKey>`

Returns the currently active `BillingKey` for an employer, if one exists.

- Returns `Optional.empty()` if the employer has no active billing key (not subscribed or subscription cancelled).

---

### `getAllActiveBillingKeys() → List<BillingKey>`

Returns all active `BillingKey` records across all employers.

- Used by `SettlementScheduler` to iterate over subscribers for monthly billing.

---

### `deactivateBillingKey(Long employerId)`

Deactivates the active billing key for an employer.

- No-op if no active key exists.
- Sets `active = false` on the found key and saves.
- Used for plan cancellation or manual admin operations.

---

## 8. PortOneApiClient

**Purpose:** WebClient-based HTTP client for PortOne V2 REST API. All external PortOne communication goes through this class.

**Base URL:** `https://api.portone.io`
**Auth header:** `Authorization: PortOne {apiSecret}` (from `portone.api-secret` in `application.yml`)

**Test mode:** Set `PORTONE_API_SECRET=v2_test_...` to enable test mode. Use test card `4111 1111 1111 1111` with any future expiry and any CVC. No real charges occur.

---

### `getPayment(String paymentId) → PortOnePaymentInfo`

Fetches a payment record from PortOne by its ID.

- `GET /payments/{paymentId}`
- On HTTP 404: throws `BusinessException(PAYMENT_NOT_FOUND)`
- On any other non-2xx: throws `BusinessException(PAYMENT_FAILED)`
- Called by `EmployerSettlementService.verifyContractPayment()` to confirm the payment exists and is PAID.

---

### `chargeBillingKey(String billingKey, long amount, String orderName, String customerId) → PortOnePaymentInfo`

Charges an amount to a stored billing key immediately.

- `POST /payments/{paymentId}/billing-key`
- Generates a unique `paymentId` with prefix `"sub-"` + UUID.
- Request body includes `billingKey`, `orderName`, `amount.total`, `currency: "KRW"`, `customer.id`, and `channelKey` (if configured).
- On any non-2xx: throws `BusinessException(PAYMENT_FAILED)`
- Called by both `SubscriptionPaymentService.processPayment()` and `chargeScheduled()`.

---

## 9. SettlementScheduler

**Purpose:** Automated cron jobs for daily disbursement and monthly subscription billing.

**Dependencies:** `AdminSettlementService`, `SubscriptionPaymentService`, `BillingKeyRepository`, `PortOneProperties`

---

### `runDailyDisbursement()` — `@Scheduled(cron = "0 0 9 * * ?")`

Runs every day at **09:00**. Delegates entirely to `AdminSettlementService.runDisbursement()`.

- Wrapped in a try/catch — if `runDisbursement()` throws an uncaught exception, it is logged and the scheduler continues normally on the next trigger.
- Can be triggered manually via `POST /api/v1/settlements/disburse/run`.

---

### `runMonthlySubscriptionBilling()` — `@Scheduled(cron = "0 0 0 1 * ?")`

Runs on the **1st of every month at 00:00**. Charges all active billing keys.

**Steps:**
1. Loads all active `BillingKey` records via `BillingKeyRepository.findByActiveTrue()`.
2. Skips if no active keys exist.
3. For each key:
   - Resolves the plan price via `resolvePlanPrice(planType)` from `PortOneProperties`.
   - **Skips FREE plan** (amount ≤ 0) with a warning log.
   - Calls `SubscriptionPaymentService.chargeScheduled(bk, amount)`.
   - Catches individual exceptions — one failure does not stop other keys from being charged.
4. Logs final summary: `"구독 자동결제 완료: 성공=X/Y"`.

---

### `resolvePlanPrice(PlanType planType) → long` *(private)*

Resolves the configured price for a plan type from `PortOneProperties`:

| Plan | Config key | Default |
|------|-----------|---------|
| PRO | `portone.subscription.plan.pro` | 29,900 KRW |
| PRIME | `portone.subscription.plan.prime` | 59,900 KRW |
| FREE | — | 0 (skipped) |

---

## Error Codes (payment-related)

| Code | Meaning |
|------|---------|
| `PAYMENT_NOT_FOUND` | PortOne `paymentId` does not exist |
| `PAYMENT_FAILED` | PortOne API returned an error |
| `PAYMENT_AMOUNT_MISMATCH` | PortOne amount ≠ calculated settlement total |
| `SETTLEMENT_NOT_FOUND` | Settlement record (employer or freelancer) not found |
| `SETTLEMENT_FORBIDDEN` | Caller does not own the requested settlement |
| `WALLET_NOT_FOUND` | Platform wallet expected but not present (ESCROW missing during disbursement) |
| `INVALID_INPUT_VALUE` | Bad enum value or invalid state transition (e.g. tax invoice on non-PAID settlement) |
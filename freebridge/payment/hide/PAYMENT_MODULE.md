# Payment Module — API & Service Summary

## Authentication

All user-facing endpoints require a JWT token in the `Authorization` header:
```
Authorization: Bearer <token>
```
The token is issued by the user module on login. The server extracts the caller's user ID from the token subject — no manual ID parameter is needed.

Admin-only endpoints (wallet platform views, settlement admin list, generate/disburse/cancel) are protected by `@PreAuthorize("hasRole('ADMIN')")`. Requests without an `ADMIN` role will receive `403 Forbidden`.

---

## Entities & Their Relationships

```
Contract (external, contract module)
  └── EmployerSettlement (one per installment month)
        └── FreelancerSettlement (1:1 with EmployerSettlement)

Wallet (one per user; separate platform wallets for ESCROW and REVENUE)
  └── WalletTransaction (append-only ledger)

SubscriptionBilling (one record per subscription payment attempt)
  └── BillingKey (stores PortOne billing key for recurring charges)
```

---

## Full Payment Flow

### 1. Contract Payment (고용주 → 계약 체결)

**Who:** Employer (EMPLOYER JWT token)

**Steps:**
1. Frontend calls PortOne SDK to make a one-time payment (full contract budget).
2. PortOne returns a `paymentId`.
3. Frontend calls **POST /api/v1/settlements/employer/verify-payment** with `paymentId` and `contractId`.
4. Backend verifies payment with PortOne V2 API.
5. On success:
   - Creates `EmployerSettlement` records — one per installment month (budget split evenly, last month absorbs remainder).
   - Creates paired `FreelancerSettlement` records (status: PENDING, scheduled for each month's paymentDay).
   - Credits `PLATFORM_ESCROW` wallet by the full total.
   - Records a DEBIT transaction on the employer's wallet.
6. Response returns total verified amount and installment count.

**Idempotent:** Calling again with the same `paymentId` returns the cached result without re-processing.

---

### 2. Freelancer Disbursement (자동 지급)

**Who:** Automated scheduler (no API call needed from frontend)

**Trigger:** Every day at 09:00 — scheduler calls `AdminSettlementService.runDisbursement()`.
**Manual trigger:** **POST /api/v1/settlements/disburse/run** (admin, no auth required currently)

**Steps for each due FreelancerSettlement (scheduledDate ≤ today, status = PENDING):**
1. Debit `PLATFORM_ESCROW` by `EmployerSettlement.totalPayment`.
2. Credit `PLATFORM_REVENUE` by `employerPlatformFee + freelancerTax`.
3. Credit freelancer's `FREELANCER` wallet by `FreelancerSettlement.netAmount`.
4. Record three `WalletTransaction` entries (escrow debit, revenue credit, freelancer credit).
5. Mark `EmployerSettlement` → DISBURSED, `FreelancerSettlement` → PAID.

**Fee breakdown per installment:**
```
billingAmount   = contractBudget / totalMonths  (last month absorbs remainder)
employerFee     = floor(billingAmount × commissionRate)
totalPayment    = billingAmount + employerFee         ← what employer paid
freelancerFee   = floor(billingAmount × commissionRate)
tax             = floor((billingAmount - freelancerFee) × 0.033)
netAmount       = billingAmount - freelancerFee - tax ← what freelancer receives
platformRevenue = employerFee + tax
```

---

### 3. Subscription Payment (구독 결제)

**Who:** Employer (EMPLOYER JWT token) — but called internally from subscription module

**Steps:**
1. Frontend calls PortOne SDK to issue a billing key (recurring card registration).
2. Subscription module calls **POST /api/v1/internal/payments/subscription** with `{ employerId, planType, amount, billingKey }`.
3. Backend calls PortOne to charge immediately.
4. On success:
   - Deactivates any existing active `BillingKey` for this employer.
   - Saves new `BillingKey` (active = true).
   - Creates `SubscriptionBilling` record (status: PAID).
   - Credits `PLATFORM_REVENUE` wallet.
5. On failure: saves `SubscriptionBilling` record (status: FAILED), returns error info.

**Recurring charge:** Every 1st of the month at 00:00, the scheduler charges all active billing keys automatically via `chargeScheduled()`. This does NOT create a new `BillingKey` — it reuses the stored one.

**Plan prices** are configured in `application.yml` under `portone.subscription.plan.*`.

---

## API Reference

### Wallet — `/api/v1/wallets`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/employer/summary` | Employer JWT | Total paid out + transaction count |
| GET | `/employer/transactions` | Employer JWT | Paginated transaction history; filter by `referenceType` |
| GET | `/freelancer/summary` | Freelancer JWT | Current balance + transaction count |
| GET | `/freelancer/transactions` | Freelancer JWT | Paginated transaction history |
| GET | `/platform/escrow` | Admin JWT (ROLE_ADMIN) | PLATFORM_ESCROW wallet balance |
| GET | `/platform/revenue` | Admin JWT (ROLE_ADMIN) | PLATFORM_REVENUE wallet balance |

`referenceType` values: `ALL`, `CONTRACT_PAYMENT`, `FREELANCER_DISBURSEMENT`, `PLATFORM_FEE`, `SUBSCRIPTION_PAYMENT`, `REFUND`

---

### Employer Settlement — `/api/v1/settlements/employer`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/` | Employer JWT | Paginated list; filter by `status`, `dateRange`, `sort` |
| GET | `/summary` | Employer JWT | Aggregate totals (paid, disbursed, cancelled counts/amounts) |
| GET | `/next` | Employer JWT | Next upcoming disbursement record |
| GET | `/{settlementId}` | Employer JWT | Full detail of one settlement |
| GET | `/{settlementId}/invoice` | Employer JWT | Invoice PDF URL (S3) |
| POST | `/verify-payment` | Employer JWT | Verify PortOne payment and generate settlement records |

**POST `/verify-payment` body:**
```json
{
  "paymentId": "portone_payment_id",
  "contractId": 1
}
```

`status` values: `ALL`, `ISSUED`, `PAID`, `DISBURSED`, `CANCELLED`
`dateRange` values: `ALL`, `LAST_3_MONTHS`, `LAST_6_MONTHS`, `LAST_1_YEAR`
`sort` values: `DUE_DATE_ASC`, `DUE_DATE_DESC`, `AMOUNT_ASC`, `AMOUNT_DESC`

---

### Freelancer Settlement — `/api/v1/settlements/freelancer`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/` | Freelancer JWT | Paginated list; filter by `status`, `dateRange`, `sort` |
| GET | `/summary` | Freelancer JWT | Pending amount, paid amount, and counts |
| GET | `/{settlementId}` | Freelancer JWT | Full detail of one settlement |
| GET | `/{settlementId}/receipt` | Freelancer JWT | Receipt PDF URL (S3) |
| POST | `/{settlementId}/tax-invoice` | Freelancer JWT | Request tax invoice (only for PAID settlements) |

**POST `/{settlementId}/tax-invoice` body:**
```json
{
  "businessRegistrationNumber": "123-45-67890",
  "companyName": "회사명",
  "email": "contact@example.com"
}
```

`status` values: `ALL`, `PENDING`, `PAID`, `CANCELLED`
`sort` values: `SCHEDULED_DATE_ASC`, `SCHEDULED_DATE_DESC`, `AMOUNT_ASC`, `AMOUNT_DESC`

---

### Admin Settlement — `/api/v1/settlements`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/admin` | Admin JWT (ROLE_ADMIN) | All settlements across all contracts, paginated |
| POST | `/generate?contractId=` | Admin JWT (ROLE_ADMIN) | Manually generate settlement records for a contract |
| POST | `/disburse/run` | Admin JWT (ROLE_ADMIN) | Manually trigger the disbursement scheduler |
| POST | `/cancel?contractId=` | Admin JWT (ROLE_ADMIN) | Cancel PAID installments, call PortOne refund API, credit employer wallet |

`/generate` is safe to call multiple times — it skips if records already exist for that contract.
`/cancel` calls PortOne `cancelPayment()` before any DB state change. On PortOne failure the operation throws and no DB records are modified.

---

### Subscription Billing — `/api/v1/subscriptions`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/billing-history` | Employer JWT | Paginated subscription payment history |

`status` values: `ALL`, `PENDING`, `PAID`, `FAILED`

---

### Internal Payment — `/api/v1/internal/payments`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/subscription` | Employer JWT | Process a subscription payment with a billing key |
| GET | `/subscription/{billingId}` | None (internal) | Look up a single billing record by ID |

**POST `/subscription` body:**
```json
{
  "planType": "PRO",
  "amount": 29000,
  "billingKey": "portone_billing_key"
}
```

> **Note:** `employerId`는 요청 바디에서 읽지 않고 JWT 인증 토큰(`@AuthenticationPrincipal`)에서 추출합니다. 바디에 `employerId`를 포함해도 무시됩니다.

`planType` values: `FREE`, `PRO`, `PRIME`

---

## Automated Schedulers

| Schedule | What it does |
|----------|-------------|
| Every day at 09:00 | Finds all FreelancerSettlements with `scheduledDate ≤ today` and `status = PENDING`, disburses each one |
| Every 1st of month at 00:00 | Finds all active BillingKeys, charges each one via PortOne, saves SubscriptionBilling record |

Both schedulers can be triggered manually via the admin endpoints above for testing.

---

## PortOne Integration Notes

- **V2 API** is used (not V1 imp_uid). The payment identifier field is `paymentId`, not `imp_uid`.
- **Test mode:** Set `PORTONE_API_SECRET=v2_test_...` in environment variables. Use test card `4111 1111 1111 1111` with any future expiry and any CVC. No real money is charged.
- **channelKey** must be set in `application.yml` under `portone.channel-key` to route to the correct PortOne channel.
- **Billing key** is issued by the frontend via PortOne SDK before calling the backend. The backend never handles raw card data.

---

## Wallet Architecture

```
EMPLOYER wallet      — virtual, tracks total paid out (debit-only records)
FREELANCER wallet    — real balance, receives netAmount on disbursement
PLATFORM_ESCROW      — holds contract funds from employer payment until disbursement
PLATFORM_REVENUE     — accumulates commissions and subscription income
```

Platform wallets (ESCROW, REVENUE) have no `ownerId` — they are singletons identified by `walletType`.

---

## Cross-Module API Boundaries

### Consumed from Contract Module

`ContractQuery` (in `contract` module, `com.fallguys.contract.api.shared`) is called by both `EmployerSettlementService` and `AdminSettlementService` to fetch contract data needed for settlement creation.

| Field | Type | Used for |
|-------|------|----------|
| `id` | Long | Settlement `contractId` |
| `budget` | long | Split across installments |
| `commissionRate` | Double | Fee calculation per installment |
| `paymentDay` | Integer | Due date day-of-month per installment |
| `startDate` | LocalDate | First installment month |
| `endDate` | LocalDate | Last installment month |
| `employerId` | Long | EmployerSettlement owner |
| `freelancerId` | Long | FreelancerSettlement owner |
| `projectName` | String | Returned in detail responses |

### Exposed to Other Modules

`SubscriptionPaymentQuery` (`com.fallguys.payment.api.shared`) is the in-process API that other modules (e.g. `subscription`) call to trigger a subscription payment without going through HTTP.

```java
public interface SubscriptionPaymentQuery {
    SubscriptionPaymentResult processSubscriptionPayment(
            Long employerId, String planType, long amount, String billingKey);
}
```

`SubscriptionPaymentResult` fields:

| Field | Type | Description |
|-------|------|-------------|
| `success` | boolean | Whether the charge succeeded |
| `billingId` | Long | Created `SubscriptionBilling` record ID |
| `planType` | String | `"PRO"` or `"PRIME"` |
| `amount` | Long | Amount charged in KRW |
| `status` | String | `"PAID"` or `"FAILED"` |
| `errorCode` | String | Non-null only on failure |
| `errorMessage` | String | Non-null only on failure |

**Usage pattern** in the calling module:
```java
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionPaymentQuery subscriptionPaymentQuery;

    public void upgradePlan(Long employerId, String planType, long amount, String billingKey) {
        SubscriptionPaymentResult result =
            subscriptionPaymentQuery.processSubscriptionPayment(employerId, planType, amount, billingKey);

        if (!result.success()) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
        // confirm plan change
    }
}
```

Implemented by `SubscriptionPaymentService`, which delegates directly to `processPayment()`.
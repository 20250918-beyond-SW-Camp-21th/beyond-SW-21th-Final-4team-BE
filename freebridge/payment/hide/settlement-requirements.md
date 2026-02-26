# Settlement & Subscription Domain Requirements

> Derived from frontend entity definitions, store logic, and UI data shapes.
> All cross-domain data access must go through each domain's interface API — direct entity/repository access from outside is not permitted.

> **Payment Model:** The employer pays the **full contract amount** (total budget × (1 + commissionRate)) upfront at contract signing via **PortOne sandbox** (mock mode — real transactions are simulated). The platform holds the funds and automatically disburses the freelancer's net payment each month on the contract's `paymentDay` via a scheduled job. No manual payment actions are needed from either party after the initial upfront charge.

---

## Table of Contents
1. [Settlement Domain](#1-settlement-domain)
   - [Entities](#11-entities)
   - [APIs](#12-apis)
   - [Service Logic](#13-service-logic)
   - [Interface APIs Exposed](#14-interface-apis-exposed-for-other-domains)
   - [Interface APIs Consumed](#15-interface-apis-consumed-from-other-domains)
2. [Wallet Domain](#2-wallet-domain)
   - [Entities](#21-entities)
   - [Money Flow](#22-money-flow)
   - [APIs](#23-apis)
3. [Subscription Payment (Payment Module Scope)](#3-subscription-payment-payment-module-scope)
   - [Entities](#31-entities)
   - [Interface API Exposed to Subscription Module](#32-interface-api-exposed-to-subscription-module)
   - [Service Logic](#33-service-logic)
4. [Cross-Domain Event Flow](#4-cross-domain-event-flow)
5. [Business Rules & Constraints](#5-business-rules--constraints)
6. [PortOne Integration Notes](#6-portone-integration-notes)

---

## 1. Settlement Domain

### 1.1 Entities

#### `EmployerSettlement` (Invoice — 고용주 청구)
Represents a monthly installment record for an active contract. All installments are created at once when the contract is signed and are immediately auto-paid via the upfront PortOne charge.

| Field               | Type       | Description                                                                     |
|:--------------------|:-----------|:--------------------------------------------------------------------------------|
| `id`                | `Long`     | PK                                                                              |
| `contractId`        | `Long`     | FK → Contract                                                                   |
| `employerId`        | `Long`     | Denormalized employer ID (from contract) — for auth checks and efficient queries |
| `freelancerId`      | `Long`     | Denormalized freelancer ID (from contract) — for efficient queries              |
| `transactionId`     | `String`   | PortOne `imp_uid` or mock transaction reference for the upfront payment         |
| `billingAmount`     | `Long`       | Installment amount (≈ `budget / months`; last installment absorbs remainder)    |
| `platformFee`       | `Long`       | `billingAmount * commissionRate` (computed and stored; last installment corrected) |
| `totalPayment`      | `Long`       | `billingAmount + platformFee` (total charged to employer)                       |
| `commissionRate`    | `BigDecimal` | Commission rate **snapshotted** at settlement creation (e.g., `0.10`)           |
| `installmentNumber` | `Int`        | Billing round number (1st, 2nd, … final)                                        |
| `status`            | `Enum`       | `ISSUED` → `WAITING_FOR_DEPOSIT` \| `PAID` → `DISBURSED` \| `CANCELLED`        |
| `invoicePdfUrl`     | `String`   | S3 path to generated invoice PDF                                                |
| `dueDate`           | `Date`     | Scheduled disbursement date (derived from contract `paymentDay` for that month) |
| `paidDate`          | `Date`     | Date the upfront payment was confirmed (set at contract signing)                |
| `createdAt`         | `DateTime` | Record creation timestamp                                                       |

**Status Lifecycle:**
```
[카드 결제]   ISSUED → PAID → DISBURSED
                                 ↘ CANCELLED

[가상계좌]   ISSUED → WAITING_FOR_DEPOSIT → PAID → DISBURSED
                             ↘ CANCELLED (입금 기한 만료)    ↘ CANCELLED (계약 해지)
```

> **Note:** For **card payments**, all installment records transition from `ISSUED` to `PAID` immediately at contract signing. For **virtual account payments**, records enter `WAITING_FOR_DEPOSIT` upon issuance; they transition to `PAID` only when PortOne fires a webhook confirming the bank deposit. The contract remains in `PENDING` state until the deposit is confirmed. The `CANCELLED` status is applied to future (not yet `DISBURSED`) installments when a contract is terminated early.

---

#### `FreelancerSettlement` (Disbursement — 프리랜서 정산)
Created automatically by the monthly disbursement scheduler on the contract's `paymentDay`. Represents the net amount disbursed to the freelancer after deductions. Since disbursement is fully automated (mock bank transfer via DB), there is no intermediate holding state.

| Field                  | Type       | Description                                                                          |
|:-----------------------|:-----------|:-------------------------------------------------------------------------------------|
| `id`                   | `Long`     | PK                                                                                   |
| `contractId`           | `Long`     | FK → Contract                                                                        |
| `employerSettlementId` | `Long`     | FK → EmployerSettlement (1:1)                                                        |
| `freelancerId`         | `Long`     | Denormalized freelancer ID — for auth checks and efficient queries                   |
| `totalAmount`          | `Long`     | Gross amount (`EmployerSettlement.billingAmount`)                                    |
| `platformFee`          | `Long`     | Platform commission deducted (`totalAmount * commissionRate`)                        |
| `tax`                  | `Long`       | Withholding tax deducted (last installment corrected to absorb rounding remainder)   |
| `netAmount`            | `Long`       | Final payout: `totalAmount - platformFee - tax`                                      |
| `commissionRate`       | `BigDecimal` | Commission rate **snapshotted** at settlement creation                               |
| `taxRate`              | `BigDecimal` | Tax rate **snapshotted** at settlement creation (e.g., `0.033`)                      |
| `installmentNumber`    | `Int`        | Corresponding billing round number                                                   |
| `status`               | `Enum`       | `PENDING` → `PAID` \| `CANCELLED`                                                   |
| `scheduledDate`        | `Date`     | The `paymentDay` date for this installment month (when auto-disbursement is expected)|
| `paidDate`             | `Date`     | Actual disbursement date (set by scheduler)                                          |
| `receiptPdfUrl`        | `String`   | S3 path to final payment receipt PDF (generated on `PAID`)                           |
| `createdAt`            | `DateTime` | Record creation timestamp                                                            |

**Status Lifecycle:**
```
PENDING (지급 예정) → PAID (지급 완료)
                   ↘ CANCELLED (계약 해지로 취소)
```

> **Note:** `PENDING` records are created at contract signing (one per installment month) and auto-transitioned to `PAID` by the scheduler on `scheduledDate`. If a contract is cancelled before disbursement, the record transitions to `CANCELLED`.

---

### 1.2 APIs

#### Employer Settlement APIs

| Method | Path | Description | Auth |
|:-------|:-----|:------------|:-----|
| `GET` | `/api/v1/settlements/employer` | List employer settlements (paginated) | Employer |
| `GET` | `/api/v1/settlements/employer/summary` | Aggregated stats (paid/disbursed counts and amounts) | Employer |
| `GET` | `/api/v1/settlements/employer/next` | Next upcoming installment to be disbursed to freelancer | Employer |
| `GET` | `/api/v1/settlements/employer/{settlementId}` | Settlement detail | Employer |
| `GET` | `/api/v1/settlements/employer/{settlementId}/invoice` | Download invoice PDF (redirect or pre-signed URL) | Employer |

**`GET /api/v1/settlements/employer` — Query Parameters:**
```
status        : "ALL" | "ISSUED" | "PAID" | "DISBURSED" | "CANCELLED"
dateRange     : "ALL" | "THIS_MONTH" | "LAST_MONTH" | "LAST_3_MONTHS"
search        : string  (project name or freelancer name)
sort          : "DUE_DATE_ASC" | "DUE_DATE_DESC" | "AMOUNT_DESC" (default: "DUE_DATE_ASC")
page          : int (default: 1)
size          : int (default: 10)
```

**`GET /api/v1/settlements/employer` — Response Body:**
```json
{
  "content": [
    {
      "id": 1,
      "contractId": 101,
      "projectName": "SaaS Dashboard Development",
      "freelancerName": "Kim Minsu",
      "billingAmount": 5000000,
      "platformFee": 500000,
      "totalPayment": 5500000,
      "installmentNumber": 1,
      "status": "ISSUED",
      "invoicePdfUrl": "https://s3.../invoice_1.pdf",
      "dueDate": "2025-02-25",
      "paidDate": null
    }
  ],
  "totalElements": 12,
  "totalPages": 2,
  "currentPage": 1
}
```

**`GET /api/v1/settlements/employer/summary` — Response Body:**
```json
{
  "totalPaidAmount": 32000000,
  "totalDisbursedAmount": 20000000,
  "paidCount": 3,
  "disbursedCount": 7,
  "cancelledCount": 1
}
```

**`GET /api/v1/settlements/employer/next` — Response Body:**
```json
{
  "id": 5,
  "contractId": 103,
  "projectName": "Mobile Payment App",
  "freelancerName": "Lee Jiyeon",
  "billingAmount": 4000000,
  "platformFee": 400000,
  "totalPayment": 4400000,
  "installmentNumber": 2,
  "scheduledDisbursementDate": "2025-03-10",
  "status": "PAID"
}
```
> Returns the next `EmployerSettlement` with `status = PAID` that has not yet been `DISBURSED`, ordered by `dueDate`. Returns `null` if all installments are disbursed or cancelled.

**`GET /api/v1/settlements/employer/{settlementId}` — Response Body:**
```json
{
  "id": 1,
  "contractId": 101,
  "projectName": "SaaS Dashboard Development",
  "freelancerName": "Kim Minsu",
  "billingAmount": 5000000,
  "platformFee": 500000,
  "commissionRate": 0.10,
  "totalPayment": 5500000,
  "installmentNumber": 1,
  "status": "PAID",
  "invoicePdfUrl": "https://s3.../invoice_1.pdf",
  "dueDate": "2025-02-25",
  "paidDate": "2025-02-20"
}
```

**`POST /api/v1/settlements/employer/{settlementId}/pay` — Request Body:**
```json
{}
```
> No request body needed; identity is inferred from the authenticated user. Returns `200 OK` with updated settlement status.

---

#### Freelancer Settlement APIs

| Method | Path | Description | Auth |
|:-------|:-----|:------------|:-----|
| `GET` | `/api/v1/settlements/freelancer` | List freelancer settlements (paginated) | Freelancer |
| `GET` | `/api/v1/settlements/freelancer/summary` | Aggregated stats (pending/paid net amounts) | Freelancer |
| `GET` | `/api/v1/settlements/freelancer/{settlementId}` | Settlement detail | Freelancer |
| `GET` | `/api/v1/settlements/freelancer/{settlementId}/receipt` | Download receipt PDF | Freelancer |
| `POST` | `/api/v1/settlements/freelancer/{settlementId}/tax-invoice` | Request tax invoice issuance | Freelancer |

**`POST /api/v1/settlements/freelancer/{settlementId}/tax-invoice` — Request Body:**
```json
{
  "businessRegistrationNumber": "123-45-67890",
  "companyName": "Kim Minsu 1인사업자",
  "email": "minsu@example.com"
}
```

**`POST /api/v1/settlements/freelancer/{settlementId}/tax-invoice` — Response Body:**
```json
{
  "taxInvoiceId": 42,
  "settlementId": 1,
  "status": "REQUESTED",
  "requestedAt": "2025-02-22T10:30:00"
}
```
> Only allowed when `FreelancerSettlement.status = PAID`. Returns `409 Conflict` if already requested.

**`GET /api/v1/settlements/freelancer` — Query Parameters:**
```
status        : "ALL" | "PENDING" | "PAID" | "CANCELLED"
dateRange     : "ALL" | "THIS_MONTH" | "LAST_MONTH" | "LAST_3_MONTHS"
search        : string  (project name or employer name)
sort          : "SCHEDULED_DATE_ASC" | "SCHEDULED_DATE_DESC" | "AMOUNT_DESC" (default: "SCHEDULED_DATE_ASC")
page          : int (default: 1)
size          : int (default: 10)
```

**`GET /api/v1/settlements/freelancer` — Response Body:**
```json
{
  "content": [
    {
      "id": 1,
      "contractId": 101,
      "employerSettlementId": 10,
      "projectName": "SaaS Dashboard Development",
      "employerName": "TechStartup Co.",
      "totalAmount": 5000000,
      "platformFee": 500000,
      "tax": 148500,
      "netAmount": 4351500,
      "installmentNumber": 1,
      "status": "PAID",
      "scheduledDate": "2025-02-15",
      "paidDate": "2025-02-15",
      "receiptPdfUrl": "https://s3.../receipt_1.pdf"
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "currentPage": 1
}
```

**`GET /api/v1/settlements/freelancer/summary` — Response Body:**
```json
{
  "pendingAmount": 8500000,
  "pendingCount": 2,
  "paidAmount": 21000000,
  "paidCount": 3
}
```

**`GET /api/v1/settlements/freelancer/{settlementId}` — Response Body:**
```json
{
  "id": 1,
  "contractId": 101,
  "employerSettlementId": 10,
  "projectName": "SaaS Dashboard Development",
  "employerName": "TechStartup Co.",
  "paymentDay": 15,
  "totalAmount": 5000000,
  "platformFee": 500000,
  "commissionRate": 0.10,
  "tax": 148500,
  "netAmount": 4351500,
  "installmentNumber": 1,
  "status": "PAID",
  "scheduledDate": "2025-02-15",
  "paidDate": "2025-02-15",
  "receiptPdfUrl": "https://s3.../receipt_1.pdf"
}
```

---

#### Admin / Internal APIs

| Method | Path | Description | Auth |
|:-------|:-----|:------------|:-----|
| `POST` | `/api/v1/settlements/generate` | Manually trigger settlement record generation for a specific contract (normally auto-triggered at contract signing) | Admin |
| `POST` | `/api/v1/settlements/disburse/run` | Manually trigger the auto-disbursement scheduler (normally runs on `paymentDay`) | Admin |
| `GET` | `/api/v1/settlements/admin` | List all settlements across all contracts (paginated, filterable by status) | Admin |

---

### 1.3 Service Logic

#### Settlement Generation (at Contract Signing)
- Triggered when a `Contract` transitions to `IN_PROGRESS` status (i.e., when both parties sign and the upfront PortOne payment is confirmed).
- Must call the **Contract domain's interface API** to retrieve: `contractId`, `budget`, `commissionRate`, `paymentDay`, `startDate`, `endDate`, `freelancerId`, `employerId`.
- Calculates `totalMonths` from `startDate` to `endDate`.
- Pre-computes correction targets for the last installment:
  - `totalBillingFee = Math.floor(budget * commissionRate)` — the exact total employer-side fee
  - `totalFreelancerFee = Math.floor(budget * commissionRate)` — same (mirrored on freelancer side)
  - `totalTax = Math.floor((budget - Math.floor(budget * commissionRate)) * 0.033)` — the exact total tax
- Creates one `EmployerSettlement` per installment:
  - `billingAmount` (installments 1..N-1): `Math.floor(budget / totalMonths)`
  - `billingAmount` (last installment): `budget - sum(billingAmount for installments 1..N-1)` — absorbs remainder so full budget is collected
  - `platformFee` (installments 1..N-1): `Math.floor(billingAmount * commissionRate)`
  - `platformFee` (last installment): `totalBillingFee - sum(platformFee for installments 1..N-1)` — absorbs fee rounding error
  - `totalPayment = billingAmount + platformFee`
  - `commissionRate` = snapshotted from `Contract.commissionRate` (stored directly on the entity)
  - `dueDate` = `year-month-min(paymentDay, lastDayOfMonth(year, month))` for each installment month (end-of-month correction)
  - `status`:
    - Card payment: `PAID` (immediately, full payment already collected via PortOne)
    - Virtual account: `WAITING_FOR_DEPOSIT` (transitions to `PAID` only when PortOne webhook confirms deposit)
  - `paidDate`:
    - Card payment: `today` (date of contract signing)
    - Virtual account: `null` until webhook confirms
  - `transactionId` = PortOne `imp_uid` (shared across all installments for the same contract)
- Creates one `FreelancerSettlement` per installment (created at the same time):
  - `totalAmount = EmployerSettlement.billingAmount`
  - `platformFee` (installments 1..N-1): `Math.floor(totalAmount * commissionRate)`
  - `platformFee` (last installment): `totalFreelancerFee - sum(platformFee for installments 1..N-1)` — absorbs rounding error
  - `tax` (installments 1..N-1): `Math.floor((totalAmount - platformFee) * taxRate)`
  - `tax` (last installment): `totalTax - sum(tax for installments 1..N-1)` — ensures sum of all taxes is exact
  - `netAmount = totalAmount - platformFee - tax`
  - `commissionRate` = snapshotted from `Contract.commissionRate`
  - `taxRate = 0.033` (snapshotted at creation time — stored for historical accuracy)
  - `status = PENDING`
  - `scheduledDate` = same as the corresponding `EmployerSettlement.dueDate`

#### PortOne Upfront Payment Flow

**Card Payment (카드 — 즉시 완료):**
1. Employer initiates card payment via PortOne SDK on the frontend.
2. On payment success, frontend sends the PortOne `imp_uid` to `POST /api/v1/settlements/verify-payment`.
3. Backend calls PortOne verification API; confirms paid amount equals `sum of all totalPayment`.
4. On verification success:
   - Settlement records created with `status = PAID`, `paidDate = today`.
   - Contract transitions to `IN_PROGRESS`.
   - Wallet transactions executed.
5. On verification failure: return `400 Bad Request`, contract stays in pending state.

**Virtual Account (가상계좌 — 입금 대기):**
1. Employer requests virtual account issuance via PortOne SDK on the frontend.
2. Frontend calls `POST /api/v1/settlements/verify-payment` with the `imp_uid`.
3. Backend calls PortOne verification API; detects `payment_method = vbank` and `status = ready`:
   - Settlement records created with `status = WAITING_FOR_DEPOSIT`, `paidDate = null`.
   - Contract stays in `PENDING` state.
   - Returns virtual account info (bank name, account number, expiry) to frontend.
4. PortOne fires a **webhook** to `POST /api/v1/portone/webhook` when the employer deposits funds.
5. Payment module verifies the webhook event (`status = paid`, amount matches):
   - All `WAITING_FOR_DEPOSIT` settlement records for that contract transition to `PAID`, `paidDate = webhook timestamp`.
   - Contract transitions to `IN_PROGRESS`.
   - Wallet transactions executed.
6. On deposit timeout or cancellation: settlements remain `WAITING_FOR_DEPOSIT`; contract stays `PENDING`.
   - Admin can manually expire or re-issue the virtual account.

> The `/api/v1/settlements/employer/{settlementId}/pay` endpoint is **removed** — payment is not per-installment. All installments are auto-PAID at contract signing.

#### Auto-Disbursement Scheduler (`PENDING` → `PAID`)
- Runs **daily** (e.g., at 00:00 KST).
- Finds all `FreelancerSettlement` records where `status = PENDING` and `scheduledDate <= today`.
- For each:
  1. Performs a mock bank transfer (DB transaction: debit platform account, credit freelancer account).
  2. Sets `FreelancerSettlement.status = PAID`, `paidDate = today`.
  3. Generates `receiptPdfUrl` and stores it.
  4. Sets the corresponding `EmployerSettlement.status = DISBURSED`.

#### Contract Cancellation → Settlement Cancellation + PortOne Partial Refund
- Triggered when a `Contract` is cancelled/terminated early (via the Contract domain's interface).
- Identifies all undisbursed installments: `FreelancerSettlement` with `status = PENDING` and their paired `EmployerSettlement` with `status = PAID`.
- **Step 1 — Call PortOne Partial Cancel API (before any DB change):**
  - Calculates refund amount: `sum(billingAmount + platformFee)` for all undisbursed installments.
  - Calls PortOne `POST /payments/cancel` with the contract's `imp_uid` and the partial `amount`.
  - **Transaction guarantee: DB status changes only proceed on PortOne cancel success.**
  - On PortOne failure: throw exception, leave all settlement records unchanged. The caller (Contract domain) must retry.
- **Step 2 — On PortOne cancel success, within a single DB transaction:**
  - Sets `FreelancerSettlement` (PENDING) → `CANCELLED`.
  - Sets `EmployerSettlement` (PAID) → `CANCELLED`.
  - Executes wallet transactions (refund):
    - `PLATFORM_ESCROW` DEBIT `billingAmount`
    - `PLATFORM_REVENUE` DEBIT `platformFee_employer`
    - `EMPLOYER wallet` CREDIT `billingAmount + platformFee_employer`

#### PDF Generation
- `invoicePdfUrl`: Generated when `EmployerSettlement` is created (at contract signing). Stored in S3.
- `receiptPdfUrl`: Generated when `FreelancerSettlement` transitions to `PAID`. Stored in S3.

---

### 1.4 Interface APIs Exposed (for other domains)

Other domains must call these endpoints instead of accessing the settlement repository directly.

| Method | Path | Description | Consumer |
|:-------|:-----|:------------|:---------|
| `GET` | `/api/v1/internal/settlements/contract/{contractId}` | Get all settlement records for a contract | Contract domain |
| `POST` | `/api/v1/internal/settlements/contract/{contractId}/cancel` | Cancel all PENDING/PAID-undisbursed settlements when a contract is terminated | Contract domain |
| `GET` | `/api/v1/internal/settlements/employer/{employerId}/stats` | Get settlement summary for an employer | User/MyPage domain |
| `GET` | `/api/v1/internal/settlements/freelancer/{freelancerId}/stats` | Get settlement summary for a freelancer | User/MyPage domain |

---

### 1.5 Interface APIs Consumed (from other domains)

The Settlement domain must consume these interface APIs and must **not** directly access other domains' databases.

| Source Domain | Method | Path | Data Retrieved |
|:-------------|:-------|:-----|:--------------|
| Contract | `GET` | `/api/v1/internal/contracts/{contractId}` | `contractId`, `budget`, `commissionRate`, `paymentDay`, `startDate`, `endDate`, `freelancerId`, `employerId`, `projectName`, `status` |
| Contract | `GET` | `/api/v1/internal/contracts/active` | All contracts with `IN_PROGRESS` status for batch settlement generation |
| User | `GET` | `/api/v1/internal/users/{userId}/name` | Freelancer/employer display names for invoice/receipt PDFs |

---

## 2. Wallet Domain

> The Wallet domain is owned by the **Payment module**. It provides the virtual ledger for all money movements across the platform. No actual bank accounts are used — all balances are mock DB values.

### 2.1 Entities

#### `Wallet`

| Field        | Type       | Description                                                                                  |
|:-------------|:-----------|:---------------------------------------------------------------------------------------------|
| `id`         | `Long`     | PK                                                                                           |
| `ownerId`    | `Long`     | User ID for EMPLOYER/FREELANCER wallets; `null` for platform-level wallets                  |
| `walletType` | `Enum`     | `EMPLOYER` \| `FREELANCER` \| `PLATFORM_ESCROW` \| `PLATFORM_REVENUE`                       |
| `balance`    | `Long`     | Current balance in KRW (always ≥ 0 for platform wallets; ledger-only for user wallets)      |
| `createdAt`  | `DateTime` | Record creation timestamp                                                                    |
| `updatedAt`  | `DateTime` | Last balance update timestamp                                                                |

**Wallet Types:**
| Type | Owner | Purpose |
|:-----|:------|:--------|
| `EMPLOYER` | One per employer | Ledger of what the employer has paid out (via PortOne). Balance is informational. |
| `FREELANCER` | One per freelancer | Accumulates net disbursements received. Balance = total earned to date. |
| `PLATFORM_ESCROW` | Platform-wide (single record) | Holds funds collected from employers that are owed to freelancers but not yet disbursed. |
| `PLATFORM_REVENUE` | Platform-wide (single record) | Holds platform earnings: commission fees from both sides + subscription payments. |

> **Two platform wallets rationale:** `PLATFORM_ESCROW` represents a **liability** (money the platform holds on behalf of freelancers). `PLATFORM_REVENUE` represents actual **earned income** (fees). Keeping them separate makes auditing and financial reporting straightforward.

---

#### `WalletTransaction`
Every money movement is recorded as an immutable ledger entry.

| Field           | Type       | Description                                                                                           |
|:----------------|:-----------|:------------------------------------------------------------------------------------------------------|
| `id`            | `Long`     | PK                                                                                                    |
| `walletId`      | `Long`     | FK → Wallet                                                                                           |
| `type`          | `Enum`     | `CREDIT` (money in) \| `DEBIT` (money out)                                                           |
| `amount`        | `Long`     | Transaction amount in KRW (always positive)                                                           |
| `referenceType` | `Enum`     | `CONTRACT_PAYMENT` \| `FREELANCER_DISBURSEMENT` \| `PLATFORM_FEE` \| `SUBSCRIPTION_PAYMENT` \| `REFUND` |
| `referenceId`   | `Long`     | ID of the related record (e.g., `EmployerSettlement.id`, `SubscriptionBilling.id`)                   |
| `description`   | `String`   | Human-readable description (e.g., "Contract #101 installment 1 disbursement")                        |
| `balanceAfter`  | `Long`     | Wallet balance snapshot immediately after this transaction                                            |
| `createdAt`     | `DateTime` | Immutable timestamp                                                                                   |

---

### 2.2 Money Flow

#### On Contract Upfront Payment (PortOne confirmed)
For each installment `i` of a contract with `totalMonths` installments:

```
Employer pays via PortOne: sum(billingAmount_i + platformFee_employer_i) for all i

→ PLATFORM_ESCROW   CREDIT  sum(billingAmount_i)             [held for future freelancer disbursement]
→ PLATFORM_REVENUE  CREDIT  sum(platformFee_employer_i)      [earned immediately as employer-side commission]
→ EMPLOYER wallet   DEBIT   total PortOne amount              [ledger entry only — records what employer paid]
```

#### On Monthly Auto-Disbursement (scheduler fires for installment `i`)
```
PLATFORM_ESCROW      DEBIT   billingAmount_i                  [release from escrow]
→ FREELANCER wallet  CREDIT  netAmount_i                      [freelancer receives net pay]
→ PLATFORM_REVENUE   CREDIT  platformFee_freelancer_i + tax_i [platform earns freelancer-side commission + withheld tax]
```

> Net check: `billingAmount_i = netAmount_i + platformFee_freelancer_i + tax_i` ✓

#### On Contract Cancellation (undisbursed installments)
```
For each CANCELLED installment:
PLATFORM_ESCROW      DEBIT   billingAmount_i                  [release escrowed amount]
→ EMPLOYER wallet    CREDIT  billingAmount_i + platformFee_employer_i  [refund to employer ledger]
→ PLATFORM_REVENUE   DEBIT   platformFee_employer_i           [reverse the already-earned employer fee]
```

#### On Subscription Upgrade Payment (PortOne confirmed)
```
Employer pays via PortOne: subscriptionAmount

→ PLATFORM_REVENUE   CREDIT  subscriptionAmount               [subscription revenue earned immediately]
→ EMPLOYER wallet    DEBIT   subscriptionAmount               [ledger entry]
```

---

### 2.3 APIs

#### Employer Wallet APIs

| Method | Path | Description | Auth |
|:-------|:-----|:------------|:-----|
| `GET` | `/api/v1/wallets/employer/summary` | Get employer's wallet summary (total paid out) | Employer |
| `GET` | `/api/v1/wallets/employer/transactions` | List employer's transaction history (paginated) | Employer |

**`GET /api/v1/wallets/employer/summary` — Response Body:**
```json
{
  "totalPaidOut": 49500000,
  "transactionCount": 9
}
```

**`GET /api/v1/wallets/employer/transactions` — Query Parameters:**
```
referenceType : "ALL" | "CONTRACT_PAYMENT" | "SUBSCRIPTION_PAYMENT" | "REFUND" (default: "ALL")
page          : int (default: 1)
size          : int (default: 10)
```

#### Freelancer Wallet APIs

| Method | Path | Description | Auth |
|:-------|:-----|:------------|:-----|
| `GET` | `/api/v1/wallets/freelancer/summary` | Get freelancer's wallet summary (total earned) | Freelancer |
| `GET` | `/api/v1/wallets/freelancer/transactions` | List freelancer's transaction history (paginated) | Freelancer |

**`GET /api/v1/wallets/freelancer/summary` — Response Body:**
```json
{
  "totalEarned": 21000000,
  "pendingAmount": 8500000,
  "transactionCount": 5
}
```

#### Admin Wallet APIs

| Method | Path | Description | Auth |
|:-------|:-----|:------------|:-----|
| `GET` | `/api/v1/wallets/platform/escrow` | View platform escrow wallet balance | Admin |
| `GET` | `/api/v1/wallets/platform/revenue` | View platform revenue wallet balance | Admin |

---

## 3. Subscription Payment (Payment Module Scope)

> **Scope boundary:** The **Subscription module** (teammate) owns `SubscriptionPlan` and `EmployerSubscription` entities and all subscription management APIs (plan listing, plan change, current plan view). The **Payment module** owns only `SubscriptionBilling` and the payment processing logic. When a subscription upgrade occurs, the Subscription module calls the Payment module's interface API to charge the employer and record the billing.

### 3.1 Entities

#### `SubscriptionBilling` (구독 결제 내역)
Owned by the Payment module. Created when a subscription upgrade payment is processed.

| Field           | Type       | Description                                              |
|:----------------|:-----------|:---------------------------------------------------------|
| `id`            | `Long`     | PK                                                       |
| `employerId`    | `Long`     | FK → Employer (User domain)                              |
| `planType`      | `Enum`     | Plan the employer is upgrading to                        |
| `amount`        | `Long`     | Amount charged (from `SubscriptionPlan.monthlyPrice`)    |
| `transactionId` | `String`   | PortOne `imp_uid` for this payment                       |
| `status`        | `Enum`     | `PENDING` \| `PAID` \| `FAILED`                          |
| `billingDate`   | `Date`     | Date the billing was attempted                           |
| `paidDate`      | `Date`     | Date payment was confirmed                               |
| `createdAt`     | `DateTime` | Record creation timestamp                                |

---

### 3.2 Interface API Exposed to Subscription Module

The Subscription module calls this endpoint when an employer upgrades their plan (cheaper → more expensive). The Payment module verifies the PortOne payment, records the billing, credits `PLATFORM_REVENUE`, and returns the result.

| Method | Path | Description | Auth |
|:-------|:-----|:------------|:-----|
| `POST` | `/api/v1/internal/payments/subscription` | Process a subscription upgrade payment | Internal (Subscription Module) |
| `GET` | `/api/v1/internal/payments/subscription/{billingId}` | Get billing record by ID | Internal |

**`POST /api/v1/internal/payments/subscription` — Request Body:**
```json
{
  "employerId": 1,
  "planType": "PRIME",
  "amount": 99000,
  "imp_uid": "imp_1234567890"
}
```
> `imp_uid` is from the PortOne payment the employer completed on the frontend before the Subscription module calls this endpoint.

**`POST /api/v1/internal/payments/subscription` — Response Body (success):**
```json
{
  "success": true,
  "billingId": 42,
  "employerId": 1,
  "planType": "PRIME",
  "amount": 99000,
  "status": "PAID",
  "paidDate": "2025-02-26"
}
```

**`POST /api/v1/internal/payments/subscription` — Response Body (failure):**
```json
{
  "success": false,
  "billingId": 43,
  "status": "FAILED",
  "errorCode": "PORTONE_VERIFICATION_FAILED",
  "message": "Paid amount (39000) does not match expected subscription price (99000)"
}
```

> On success, the Subscription module should proceed to update `EmployerSubscription.planType`. On failure, the plan change must be rolled back.

#### Billing History API (Employer-Facing — served by Payment module)

| Method | Path | Description | Auth |
|:-------|:-----|:------------|:-----|
| `GET` | `/api/v1/subscriptions/billing-history` | List past subscription billing records for the authenticated employer | Employer |

**Query Parameters:**
```
status   : "ALL" | "PAID" | "FAILED" (default: "ALL")
page     : int (default: 1)
size     : int (default: 10)
```

**Response Body:**
```json
{
  "content": [
    {
      "id": 42,
      "planType": "PRIME",
      "amount": 99000,
      "status": "PAID",
      "billingDate": "2025-02-26",
      "paidDate": "2025-02-26"
    }
  ],
  "totalElements": 3,
  "totalPages": 1,
  "currentPage": 1
}
```

---

### 3.3 Service Logic

#### Subscription Upgrade Payment Flow
1. Employer completes PortOne payment on the frontend → receives `imp_uid`.
2. Subscription module calls `POST /api/v1/internal/payments/subscription` with `{ employerId, planType, amount, imp_uid }`.
3. Payment module:
   a. Creates a `SubscriptionBilling` record with `status = PENDING`.
   b. Calls PortOne verification API to confirm `imp_uid` and paid amount.
   c. **On success:**
      - Sets `SubscriptionBilling.status = PAID`, `paidDate = today`, `transactionId = imp_uid`.
      - Credits `PLATFORM_REVENUE` wallet by `amount`.
      - Creates a DEBIT `WalletTransaction` on the employer's wallet.
      - Returns success response to Subscription module.
   d. **On failure:**
      - Sets `SubscriptionBilling.status = FAILED`.
      - Returns failure response to Subscription module (plan change is not applied).

> **Only upgrades are paid.** Downgrading to a cheaper plan (or to FREE) does not trigger a payment and therefore does not call the Payment module. The Subscription module handles downgrades independently.

---

## 4. Cross-Domain Event Flow

### Contract Signing → Upfront Payment → Settlement + Wallet
```
Employer (Frontend)
  └─ Initiates PortOne payment for full contract amount
       └─ PortOne sandbox returns imp_uid
            └─ Payment/Settlement Domain (POST /api/v1/settlements/verify-payment)
                 └─ Verifies payment with PortOne API
                 └─ On success:
                      └─ Creates EmployerSettlement records (status: PAID)
                      └─ Creates FreelancerSettlement records (status: PENDING)
                      └─ Generates invoicePdfUrl for each installment
                      └─ Wallet transactions:
                           PLATFORM_ESCROW  CREDIT  sum(billingAmounts)
                           PLATFORM_REVENUE CREDIT  sum(platformFees_employer)
                           EMPLOYER wallet  DEBIT   total PortOne amount
                      └─ Notifies Contract Domain to set status IN_PROGRESS
```

### Scheduled Auto-Disbursement → Completion + Wallet
```
Scheduler (daily at 00:00 KST)
  └─ Payment/Settlement Domain
       └─ Finds FreelancerSettlement where status=PENDING and scheduledDate <= today
       └─ For each:
            └─ Wallet transactions:
                 PLATFORM_ESCROW     DEBIT   billingAmount
                 FREELANCER wallet   CREDIT  netAmount
                 PLATFORM_REVENUE    CREDIT  platformFee_freelancer + tax
            └─ FreelancerSettlement: PENDING → PAID
            └─ Generates receiptPdfUrl
            └─ EmployerSettlement: PAID → DISBURSED
```

### Contract Cancellation → Settlement Cancellation + Refund
```
Contract Domain
  └─ Contract status changes to CANCELLED/TERMINATED
       └─ POST /api/v1/internal/settlements/contract/{contractId}/cancel
            └─ Payment/Settlement Domain
                 └─ For each undisbursed installment:
                      └─ FreelancerSettlement (PENDING) → CANCELLED
                      └─ EmployerSettlement (PAID) → CANCELLED
                      └─ Wallet transactions (refund):
                           PLATFORM_ESCROW     DEBIT   billingAmount
                           PLATFORM_REVENUE    DEBIT   platformFee_employer
                           EMPLOYER wallet     CREDIT  billingAmount + platformFee_employer
```

### Subscription Upgrade → Payment Module → Wallet
```
Employer (Frontend)
  └─ Completes PortOne payment for new plan price
       └─ Subscription Module
            └─ POST /api/v1/internal/payments/subscription
                 └─ Payment Module
                      └─ Verifies PortOne payment
                      └─ On success:
                           └─ SubscriptionBilling: PENDING → PAID
                           └─ Wallet transactions:
                                PLATFORM_REVENUE  CREDIT  subscriptionAmount
                                EMPLOYER wallet   DEBIT   subscriptionAmount
                           └─ Returns { success: true, billingId, ... } to Subscription Module
                      └─ On failure:
                           └─ SubscriptionBilling: PENDING → FAILED
                           └─ Returns { success: false, errorCode, ... } to Subscription Module
                                └─ Subscription Module: plan change rolled back
```

---

## 5. Business Rules & Constraints

### Settlement Rules
- One `FreelancerSettlement` is created per `EmployerSettlement` (1:1 relationship). Both are created together at contract signing.
- **Upfront payment model:** The employer pays the total contract amount (`sum of all totalPayment`) in a single PortOne transaction at contract signing. All `EmployerSettlement` records start with `status = PAID`.
- **Auto-disbursement:** `FreelancerSettlement` records are paid automatically by the scheduler on their `scheduledDate`. No manual admin action is needed.
- The `commissionRate` used in settlement calculations must come from the `Contract` entity (the snapshotted value at contract creation), not from the current subscription plan.
- Tax rate is fixed at **3.3%** of `(totalAmount - platformFee)`.
- Settlement amounts are always **floored** (Math.floor) — no rounding up.
- **Budget remainder rule (last installment correction):** For all installments, amounts are computed with `Math.floor`. The **last installment** corrects accumulated rounding errors for `billingAmount`, `platformFee` (both employer and freelancer sides), and `tax` by computing each as `total - sum(previous installments)`. This guarantees:
  - `sum(billingAmount) = budget`
  - `sum(platformFee_employer) = Math.floor(budget * commissionRate)`
  - `sum(tax) = Math.floor((budget - Math.floor(budget * commissionRate)) * taxRate)`
  - `PLATFORM_ESCROW.balance` reaches exactly **0** after all installments are disbursed.
- **End-of-month date correction:** `scheduledDate` (and `dueDate`) are calculated as `min(paymentDay, lastDayOfMonth(year, month))`. For example, if `paymentDay = 31` and the month is February, `scheduledDate = Feb 28 (or 29)`. The scheduler query `scheduledDate <= today` then guarantees no payment is silently skipped.
- **Cancellation rule:** When a contract is cancelled, all `FreelancerSettlement` records with `status = PENDING` and corresponding `EmployerSettlement` records with `status = PAID` (not yet DISBURSED) are set to `CANCELLED`. **PortOne partial cancel must succeed before any DB status change** (see Contract Cancellation service logic).
- **Rate snapshot rule:** `commissionRate` and `taxRate` are stored directly on both `EmployerSettlement` and `FreelancerSettlement` at creation time. Historical records must never be recalculated using current platform rates — always use the stored snapshot.
- A freelancer's settlement `netAmount` formula:
  ```
  netAmount = totalAmount - platformFee - tax
  = billingAmount - floor(billingAmount * commissionRate) - floor((billingAmount - floor(billingAmount * commissionRate)) * 0.033)
  ```

### Subscription Payment Rules
- The Payment module only processes **upgrade payments** (cheaper → more expensive plan). Downgrades do not trigger payment and are handled entirely by the Subscription module.
- A `SubscriptionBilling` record is always created before PortOne verification, starting as `PENDING`. It transitions to `PAID` or `FAILED` based on PortOne result.
- `FREE` plan employers are never charged. No `SubscriptionBilling` record is created for `FREE` plan (the Subscription module manages that separately).
- A failed payment (`FAILED`) means the plan change is not applied. The Subscription module must roll back any partial plan change upon receiving a failure response.
- `commissionRate` used in settlement calculations is always the value snapshotted on the `Contract` at creation time — not the current plan.

### Wallet Rules
- Every employer and freelancer gets exactly one wallet of their respective type, created automatically when they register.
- Two platform wallets exist (`PLATFORM_ESCROW`, `PLATFORM_REVENUE`) — created once at system initialization.
- `WalletTransaction` records are immutable — never updated or deleted.
- `PLATFORM_ESCROW.balance` must always equal `sum of billingAmount for all PENDING FreelancerSettlements`. If they diverge, it signals a data integrity error.
- Employer and freelancer wallet balances are informational ledger values; they do not constrain spending (payment is always via PortOne).

### PDF / Document Rules
- Invoice PDF (`invoicePdfUrl`) is generated at `EmployerSettlement` creation and must include: project name, freelancer name, installment number, billing amount, platform fee, total payment, and due date.
- Receipt PDF (`receiptPdfUrl`) is generated at `FreelancerSettlement` disbursement and must include: project name, employer name, installment number, gross amount, platform fee, tax, net amount, and paid date.

### Access Control
- An employer can only view their own `EmployerSettlement` records (ownership verified via `employerId` on the entity).
- A freelancer can only view their own `FreelancerSettlement` records (ownership verified via `freelancerId` on the entity).
- Settlement generation, manual disbursement trigger, and admin list are admin-only operations.
- All cross-domain data access is strictly through interface APIs — no direct repository or entity access from outside the domain boundary.

---

## 6. PortOne Integration Notes

> **Mode:** PortOne sandbox (developer/test mode). No real money is transferred. Transactions are simulated and can be verified against PortOne's test API.

### Payment Verification Endpoint (Internal)
| Method | Path | Description | Auth |
|:-------|:-----|:------------|:-----|
| `POST` | `/api/v1/settlements/verify-payment` | Verify PortOne payment and trigger contract activation + settlement generation | Employer |

**Request Body:**
```json
{
  "imp_uid": "imp_1234567890",
  "contractId": 101
}
```

**Response Body (success):**
```json
{
  "success": true,
  "contractId": 101,
  "totalVerifiedAmount": 49500000,
  "installmentsCreated": 9
}
```

### PortOne Verification Flow
1. Frontend completes PortOne payment → receives `imp_uid`.
2. Frontend calls `POST /api/v1/settlements/verify-payment` with `imp_uid` and `contractId`.
3. Backend calls PortOne's [GET /payments/{imp_uid}](https://developers.portone.io) API to get the paid amount.
4. Verifies the paid amount equals the expected total: `sum of (billingAmount + platformFee)` across all installments.
5. On match: triggers contract activation and settlement generation.
6. On mismatch: returns `400 Bad Request` — possible fraud or test data error.

### Idempotency
- Store `transactionId` (`imp_uid`) on all `EmployerSettlement` records for that contract.
- If `verify-payment` is called twice with the same `imp_uid`, check if settlements already exist for that contract and return the existing result (idempotent).

### Key Fields Stored from PortOne

| Field | Stored On | Description |
|:------|:----------|:------------|
| `imp_uid` | `EmployerSettlement.transactionId` | PortOne transaction ID for contract upfront payment |
| `imp_uid` | `SubscriptionBilling.transactionId` | PortOne transaction ID for subscription upgrade payment |
| `paid_at` | `EmployerSettlement.paidDate` | Payment confirmation timestamp for contract |
| `paid_at` | `SubscriptionBilling.paidDate` | Payment confirmation timestamp for subscription |

### Idempotency for Subscription Payments
- If `POST /api/v1/internal/payments/subscription` is called twice with the same `imp_uid`, check if a `SubscriptionBilling` with that `transactionId` already exists and return the existing result.

### Virtual Account Webhook
- **Endpoint:** `POST /api/v1/portone/webhook` (must be publicly accessible; registered in PortOne dashboard)
- **Trigger:** PortOne calls this endpoint when a virtual account deposit is confirmed.
- **Verification steps:**
  1. Parse `imp_uid` and `merchant_uid` from webhook payload.
  2. Call PortOne verification API to confirm `status = paid` and amount matches expected total.
  3. Find all `EmployerSettlement` records with `transactionId = imp_uid` and `status = WAITING_FOR_DEPOSIT`.
  4. Transition them to `PAID`; set `paidDate = webhook event timestamp`.
  5. Transition contract to `IN_PROGRESS`.
  6. Execute wallet transactions (same as card payment flow).
- **Idempotency:** If webhook fires twice for the same `imp_uid`, check existing statuses before processing — skip if already `PAID`.
- **Security:** Validate the webhook source using PortOne's signature header before processing.

### Partial Cancel (Contract Cancellation Refund)
- **PortOne endpoint:** `POST https://api.iamport.kr/payments/cancel`
- **Key parameters:**
  - `imp_uid`: the original transaction ID stored on `EmployerSettlement.transactionId`
  - `amount`: `sum(billingAmount + platformFee)` for all undisbursed installments (partial cancel)
  - `reason`: e.g., `"계약 해지로 인한 환불"`
- **Response handling:**
  - On success (`code = 0`): proceed with DB status updates and wallet transactions.
  - On failure: surface the PortOne error to the Contract domain. Do not change any DB state.
- **Constraint:** PortOne's maximum cancellable amount = original payment amount minus already-cancelled amount. Disbursed installments are non-refundable (already transferred to freelancer's mock account).

### Expected Amounts for Verification
| Payment Type | Expected Amount |
|:-------------|:----------------|
| Contract upfront | `sum of totalPayment` across all installments for the contract |
| Subscription upgrade | `SubscriptionPlan.monthlyPrice` for the target `planType` |
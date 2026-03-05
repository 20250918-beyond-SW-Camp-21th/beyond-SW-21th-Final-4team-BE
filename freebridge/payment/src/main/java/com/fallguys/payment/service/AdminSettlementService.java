package com.fallguys.payment.service;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.common.api.contract.ContractInfo;
import com.fallguys.common.api.contract.ContractQuery;
import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.api.web.dto.CancellationResult;
import com.fallguys.payment.entity.*;
import com.fallguys.payment.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSettlementService {

        private final EmployerSettlementRepository employerSettlementRepository;
        private final FreelancerSettlementRepository freelancerSettlementRepository;
        private final WalletRepository walletRepository;
        private final ContractQuery contractQuery;
        private final EmployerSettlementService employerSettlementService;
        private final AdminSettlementDisbursementService adminSettlementDisbursementService;

        /**
         * 계약 정산 레코드 수동 생성 (포트원 검증 없이, 어드민/테스트 용도)
         */
        @Transactional
        public void generateSettlements(Long contractId) {
                ContractInfo contract = contractQuery.getContractInfo(contractId);

                List<EmployerSettlement> existing = employerSettlementRepository.findByContractId(contractId);
                if (!existing.isEmpty()) {
                        log.warn("계약 #{} 에 대한 정산 레코드가 이미 존재합니다. 건너뜁니다.", contractId);
                        return;
                }

                employerSettlementService.createSettlementRecords(contract, "MANUAL-" + contractId,
                                contract.employerId());
                log.info("계약 #{} 정산 레코드 수동 생성 완료", contractId);
        }

        /**
         * 프리랜서 월별 정산 실행
         * scheduledDate <= 오늘 이고 PENDING 상태인 FreelancerSettlement를 처리
         * PLATFORM_ESCROW → PLATFORM_REVENUE + FREELANCER 지갑으로 이체 (DB 처리)
         */
        @Transactional
        public void runDisbursement() {
                LocalDate today = LocalDate.now();
                List<FreelancerSettlement> pendingList = freelancerSettlementRepository
                                .findByStatusAndScheduledDateLessThanEqualWithLock(FreelancerSettlementStatus.PENDING,
                                                today);

                if (pendingList.isEmpty()) {
                        log.info("지급 대상 정산 없음 (기준일: {})", today);
                        return;
                }

                Wallet escrowWallet = walletRepository.findByWalletType(WalletType.PLATFORM_ESCROW)
                                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
                Wallet revenueWallet = walletRepository.findByWalletType(WalletType.PLATFORM_REVENUE)
                                .orElseGet(() -> {
                                        Wallet w = new Wallet();
                                        w.setWalletType(WalletType.PLATFORM_REVENUE);
                                        w.setBalance(0L);
                                        return walletRepository.save(w);
                                });

                int successCount = 0;
                for (FreelancerSettlement fs : pendingList) {
                        try {
                                adminSettlementDisbursementService.processSingleDisbursement(fs, escrowWallet,
                                                revenueWallet);
                                successCount++;
                        } catch (Exception e) {
                                log.error("정산 처리 실패: freelancerSettlementId={}, error={}", fs.getId(), e.getMessage());
                        }
                }

                log.info("정산 실행 완료: 성공={}/{}", successCount, pendingList.size());
        }

        /**
         * 계약 취소 — 에스크로에 묶인 PAID 회차만 환불 처리
         * DISBURSED 회차는 이미 프리랜서에게 지급됐으므로 취소 대상에서 제외
         */
        public CancellationResult cancelContractSettlements(Long contractId) {
                // AdminSettlementService는 EmployerSettlementService의 정교한 취소 로직을 재사용합니다.
                // 먼저 해당 계약의 고용주 ID를 조회합니다.
                List<EmployerSettlement> settlements = employerSettlementRepository.findByContractId(contractId);
                if (settlements.isEmpty()) {
                        return new CancellationResult(contractId, 0, 0L);
                }
                Long employerId = settlements.get(0).getEmployerId();

                // EmployerSettlementService가 제공하는 안전한 취소 프로세스를 호출합니다.
                employerSettlementService.cancelAndRefund(contractId, employerId, "관리자 계약 취소");

                // 결과를 집계하여 반환합니다 (이미 취소된 상태이므로 CANCELLED 기준으로 집계)
                List<EmployerSettlement> cancelled = employerSettlementRepository.findByContractId(contractId).stream()
                                .filter(e -> e.getStatus() == EmployerSettlementStatus.CANCELLED)
                                .toList();
                long refundTotal = cancelled.stream().mapToLong(EmployerSettlement::getTotalPayment).sum();

                return new CancellationResult(contractId, cancelled.size(), refundTotal);
        }

        @Transactional(readOnly = true)
        public PageResponse<EmployerSettlementItem> listAllSettlements(String status, int page, int size) {
                Pageable pageable = PageRequest.of(Math.max(0, page - 1), size,
                                Sort.by(Sort.Direction.DESC, "createdAt"));
                Page<EmployerSettlement> pageResult;

                if (!"ALL".equalsIgnoreCase(status)) {
                        EmployerSettlementStatus statusEnum = EmployerSettlementStatus.valueOf(status);
                        pageResult = employerSettlementRepository.findByStatus(statusEnum, pageable);
                } else {
                        pageResult = employerSettlementRepository.findAll(pageable);
                }

                List<EmployerSettlementItem> items = pageResult.getContent().stream()
                                .map(e -> new EmployerSettlementItem(
                                                e.getId(), e.getContractId(), null, null,
                                                e.getBillingAmount(), e.getPlatformFee(), e.getTotalPayment(),
                                                e.getInstallmentNumber(), e.getStatus().name(),
                                                e.getInvoicePdfUrl(), e.getDueDate(), e.getPaidDate()))
                                .toList();

                return new PageResponse<>(items, pageResult.getTotalElements(),
                                pageResult.getTotalPages(), page);
        }
}
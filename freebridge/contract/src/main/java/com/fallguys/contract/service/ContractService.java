package com.fallguys.contract.service;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.contract.api.shared.ContractActivatedEvent;
import com.fallguys.contract.api.web.dto.*;
import com.fallguys.contract.api.web.PaginationInfo;
import com.fallguys.contract.entity.Contract;
import com.fallguys.contract.entity.ContractStatus;
import com.fallguys.contract.repository.ContractRepository;
import com.fallguys.mypage.repository.employer.EmployerRepository;
import com.fallguys.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ContractService {

    // TODO: 구독 모듈 생성 후 모듈에서 api로 수수료 불러오기로 수정
    private static final double DEFAULT_COMMISSION_RATE = 0.05;

    private final ContractRepository contractRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ContractPdfService contractPdfService;
    private final UserRepository userRepository;
    private final EmployerRepository employerRepository;


    public ContractResponse createContract(CreateContractRequest req, Long employerId) {
        Contract contract = new Contract();
        contract.setProjectName(req.getProjectName());
        contract.setFreelancerId(req.getFreelancerId());
        contract.setEmployerId(employerId);
        contract.setRelatedJobId(req.getRelatedJobId());
        contract.setRelatedApplicationId(req.getRelatedApplicationId());
        contract.setRelatedProposalId(req.getRelatedProposalId());
        contract.setStartDate(req.getStartDate());
        contract.setEndDate(req.getEndDate());
        contract.setBudget(req.getBudget());
        contract.setPaymentDay(req.getPaymentDay());
        contract.setCommissionRate(DEFAULT_COMMISSION_RATE);
        contract.setStatus(ContractStatus.WAITING_SIGNATURE);

        contract.setJobDescription(req.getJobDescription());
        contract.setWorkLocation(req.getWorkLocation());
        contract.setWorkStartTime(req.getWorkStartTime());
        contract.setWorkEndTime(req.getWorkEndTime());
        contract.setBreakStartTime(req.getBreakStartTime());
        contract.setBreakEndTime(req.getBreakEndTime());
        contract.setWorkDaysPerWeek(req.getWorkDaysPerWeek());
        contract.setWeeklyHoliday(req.getWeeklyHoliday());

        contract.setEmployerBusinessName(req.getEmployerBusinessName());
        contract.setEmployerAddress(req.getEmployerAddress());
        contract.setEmployerCEO(req.getEmployerCEO());
        contract.setFreelancerAddress(req.getFreelancerAddress());
        contract.setFreelancerPhone(req.getFreelancerPhone());

        if (req.getEmployerSignature() != null && !req.getEmployerSignature().isBlank()) {
            contract.signBy("EMPLOYER", req.getEmployerSignature());
        }

        Contract saved = contractRepository.save(contract);
        saved.setContractId(saved.getId() + 1000L);

        String pdfUrl = contractPdfService.generateContractPdf(saved);
        saved.setContractPdfUrl(pdfUrl);

        // Feature 5: AI 계약서 법률 검토 (비동기 이벤트 발행)
        try {
            byte[] pdfBytes = contractPdfService.generateContractPdfBytes(saved);
            saved.setAiLegalAdvice("AI가 계약서의 독소 조항과 법률 위반 사항을 분석하고 있습니다...");
            log.info(
                    "계약 AI 분석 요청 이벤트를 발행합니다. contractId={}, externalContractId={}, pdfBytes={}",
                    saved.getId(),
                    saved.getContractId(),
                    pdfBytes.length
            );
            eventPublisher.publishEvent(new com.fallguys.common.event.ContractAIAnalysisRequestedEvent(saved.getId(), pdfBytes));
        } catch (Exception e) {
            log.error("AI 계약서 분석용 PDF 생성 실패: contractId={}", saved.getContractId(), e);
            saved.setAiLegalAdvice("AI 분석 준비에 실패했습니다.");
        }

        saved = contractRepository.save(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ContractListResponse listContracts(Long userId, String role,
            List<String> statuses, String search, int page, int limit) {

        List<Contract> all;
        if (role != null && role.equalsIgnoreCase("EMPLOYER")) {
            all = contractRepository.findByEmployerIdOrderByIdDesc(userId);
        } else if (role != null && role.equalsIgnoreCase("FREELANCER")) {
            all = contractRepository.findByFreelancerIdOrderByIdDesc(userId);
        } else {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (statuses != null && !statuses.isEmpty()) {
            all = all.stream()
                    .filter(c -> c.getStatus() != null && statuses.contains(c.getStatus().name()))
                    .collect(Collectors.toList());
        }

        if (search != null && !search.isBlank()) {
            String lower = search.toLowerCase();
            all = all.stream()
                    .filter(c -> c.getProjectName().toLowerCase().contains(lower))
                    .collect(Collectors.toList());
        }

        int total = all.size();
        int totalPages = total == 0 ? 1 : (int) Math.ceil((double) total / limit);
        int fromIndex = Math.min((page - 1) * limit, total);
        int toIndex = Math.min(fromIndex + limit, total);

        List<ContractSummary> items = all.subList(fromIndex, toIndex).stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        return ContractListResponse.builder()
                .items(items)
                .pagination(new PaginationInfo(page, limit, total, totalPages))
                .build();
    }

    @Transactional(readOnly = true)
    public ContractResponse getContract(Long contractId, Long userId) {
        Contract contract = findByContractId(contractId);
        validateOwnership(contract, userId);
        return toResponse(contract);
    }

    public ContractResponse requestAiLegalReview(Long contractId, Long userId) {
        Contract contract = findByContractId(contractId);
        validateOwnership(contract, userId);

        try {
            byte[] pdfBytes = contractPdfService.generateContractPdfBytes(contract);
            contract.setAiLegalAdvice("AI 법률 검토를 다시 진행하고 있습니다...");
            Contract saved = contractRepository.save(contract);
            log.info(
                    "계약 AI 분석 요청 이벤트를 다시 발행합니다. contractId={}, externalContractId={}, userId={}, pdfBytes={}",
                    saved.getId(),
                    saved.getContractId(),
                    userId,
                    pdfBytes.length
            );
            eventPublisher.publishEvent(
                    new com.fallguys.common.event.ContractAIAnalysisRequestedEvent(saved.getId(), pdfBytes)
            );
            return toResponse(saved);
        } catch (Exception e) {
            log.error("AI 법률 검토 재요청 실패: contractId={}", contractId, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public ContractResponse sign(Long contractId, SignContractRequest request, String role, Long userId) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String signature = request.getSignature();
        if (signature == null || signature.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Contract contract = findByContractId(contractId);
        validateOwnership(contract, userId);
        contract.signBy(role, signature);

        // 프리랜서 서명 시 주소/연락처 업데이트 (계약 생성 시 미입력 가능 → 서명 시 확정)
        if ("FREELANCER".equalsIgnoreCase(role)) {
            if (request.getFreelancerAddress() != null && !request.getFreelancerAddress().isBlank()) {
                contract.setFreelancerAddress(request.getFreelancerAddress());
            }
            if (request.getFreelancerPhone() != null && !request.getFreelancerPhone().isBlank()) {
                contract.setFreelancerPhone(request.getFreelancerPhone());
            }
        }

        if (contract.isActivatable()) {
            contract.activate();

            // Both parties have signed — generate the signed PDF with embedded signature images
            String signedPdfUrl = contractPdfService.generateSignedPdf(contract);
            contract.setSignedPdfUrl(signedPdfUrl);

            Contract saved = contractRepository.save(contract);

            // Publish the event only after the current transaction has committed so that
            // async or transactional listeners never observe uncommitted contract data.
            final Long activatedContractId = saved.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventPublisher.publishEvent(new ContractActivatedEvent(ContractService.this, activatedContractId));
                }
            });

            return toResponse(saved);
        }

        return toResponse(contractRepository.save(contract));
    }

    public ContractResponse complete(Long contractId, Long userId) {
        Contract contract = findByContractId(contractId);
        validateOwnership(contract, userId);
        contract.complete();
        return toResponse(contractRepository.save(contract));
    }

    public ContractResponse reject(Long contractId, Long userId) {
        Contract contract = findByContractId(contractId);
        validateOwnership(contract, userId);
        contract.reject();
        return toResponse(contractRepository.save(contract));
    }

    @Transactional(readOnly = true)
    public String getPdfDownloadUrl(Long contractId, Long userId) {
        Contract contract = findByContractId(contractId);
        validateOwnership(contract, userId);
        String key = contract.getSignedPdfUrl() != null
                ? contract.getSignedPdfUrl()
                : contract.getContractPdfUrl();
        if (key == null) {
            throw new BusinessException(ErrorCode.CONTRACT_NOT_FOUND);
        }
        return contractPdfService.generatePresignedUrl(key);
    }

    private Contract findByContractId(Long contractId) {
        return contractRepository.findByContractId(contractId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTRACT_NOT_FOUND));
    }

    // TODO: 유저 모듈 완성되면 @Authentication으로 수정
    private void validateOwnership(Contract contract, Long userId) {
        if (!contract.getEmployerId().equals(userId) && !contract.getFreelancerId().equals(userId)) {
            throw new BusinessException(ErrorCode.CONTRACT_FORBIDDEN);
        }
    }

    private String getFreelancerName(Long userId) {
        if (userId == null) {
            return null;
        }

        return userRepository.findById(userId)
                .map(user -> user.getName())
                .orElse("사용자 #" + userId);
    }

    private String getEmployerDisplayName(Long employerId) {
        if (employerId == null) {
            return null;
        }

        return employerRepository.findByUserId(employerId)
                .map(employer -> employer.getCompanyName())
                .filter(name -> name != null && !name.isBlank())
                .orElseGet(() -> userRepository.findById(employerId)
                        .map(user -> user.getName())
                        .orElse("사용자 #" + employerId));
    }

    private ContractResponse toResponse(Contract c) {
        boolean employerSigned = hasSignature(c.getEmployerSignature());
        boolean freelancerSigned = hasSignature(c.getFreelancerSignature());
        return ContractResponse.builder()
                .id(c.getId())
                .contractId(c.getContractId())
                .projectName(c.getProjectName())
                .freelancerId(c.getFreelancerId())
                .employerId(c.getEmployerId())
                .relatedJobId(c.getRelatedJobId())
                .relatedApplicationId(c.getRelatedApplicationId())
                .relatedProposalId(c.getRelatedProposalId())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .budget(c.getBudget())
                .commissionRate(c.getCommissionRate())
                .paymentDay(c.getPaymentDay())
                .contractPdfUrl(c.getContractPdfUrl())
                .signedPdfUrl(c.getSignedPdfUrl())
                .signedDate(c.getSignedDate())
                .aiLegalAdvice(c.getAiLegalAdvice())
                .jobDescription(c.getJobDescription())
                .workLocation(c.getWorkLocation())
                .workStartTime(c.getWorkStartTime())
                .workEndTime(c.getWorkEndTime())
                .breakStartTime(c.getBreakStartTime())
                .breakEndTime(c.getBreakEndTime())
                .workDaysPerWeek(c.getWorkDaysPerWeek())
                .weeklyHoliday(c.getWeeklyHoliday())
                .employerBusinessName(c.getEmployerBusinessName())
                .employerAddress(c.getEmployerAddress())
                .employerCEO(c.getEmployerCEO())
                .freelancerAddress(c.getFreelancerAddress())
                .freelancerPhone(c.getFreelancerPhone())
                .employerSignature(c.getEmployerSignature())
                .employerSignedDate(c.getEmployerSignedDate())
                .freelancerSignature(c.getFreelancerSignature())
                .freelancerSignedDate(c.getFreelancerSignedDate())
                .employerSigned(employerSigned)
                .freelancerSigned(freelancerSigned)
                .freelancerName(getFreelancerName(c.getFreelancerId()))
                .employerName(getEmployerDisplayName(c.getEmployerId()))
                .build();
    }

    private ContractSummary toSummary(Contract c) {
        return ContractSummary.builder()
                .id(c.getId())
                .contractId(c.getContractId())
                .projectName(c.getProjectName())
                .freelancerId(c.getFreelancerId())
                .employerId(c.getEmployerId())
                .relatedJobId(c.getRelatedJobId())
                .relatedApplicationId(c.getRelatedApplicationId())
                .relatedProposalId(c.getRelatedProposalId())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .budget(c.getBudget())
                .employerSigned(hasSignature(c.getEmployerSignature()))
                .freelancerSigned(hasSignature(c.getFreelancerSignature()))
                .freelancerName(getFreelancerName(c.getFreelancerId()))
                .employerName(getEmployerDisplayName(c.getEmployerId()))
                .build();
    }

    private boolean hasSignature(String signature) {
        return signature != null && !signature.isBlank();
    }
}

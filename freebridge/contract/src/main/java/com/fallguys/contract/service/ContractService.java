package com.fallguys.contract.service;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.contract.api.shared.ContractActivatedEvent;
import com.fallguys.contract.api.web.dto.*;
import com.fallguys.contract.api.web.PaginationInfo;
import com.fallguys.contract.entity.Contract;
import com.fallguys.contract.entity.ContractStatus;
import com.fallguys.contract.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional
public class ContractService {

    // TODO: 구독 모듈 생성 후 모듈에서 api로 수수료 불러오기로 수정
    private static final double DEFAULT_COMMISSION_RATE = 0.05;

    private final ContractRepository contractRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ContractPdfService contractPdfService;

    public ContractResponse createContract(CreateContractRequest req, Long employerId) {
        Contract contract = new Contract();
        contract.setProjectName(req.getProjectName());
        contract.setFreelancerId(req.getFreelancerId());
        contract.setEmployerId(employerId);
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

    public ContractResponse sign(Long contractId, String signature, String role, Long userId) {
        Contract contract = findByContractId(contractId);
        validateOwnership(contract, userId);
        contract.signBy(role, signature);

        if (contract.isActivatable()) {
            contract.activate();

            // Both parties have signed — generate the signed PDF with embedded signature images
            String signedPdfUrl = contractPdfService.generateSignedPdf(contract);
            contract.setSignedPdfUrl(signedPdfUrl);

            Contract saved = contractRepository.save(contract);
            eventPublisher.publishEvent(new ContractActivatedEvent(this, saved.getId()));
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

    // TODO: 유저 모듈 완성되면 api 콜로 수정하기
    private String getMockUserName(Long userId) {
        if (userId == null) return null;
        return "사용자 #" + userId;
    }

    private ContractResponse toResponse(Contract c) {
        return ContractResponse.builder()
                .id(c.getId())
                .contractId(c.getContractId())
                .projectName(c.getProjectName())
                .freelancerId(c.getFreelancerId())
                .employerId(c.getEmployerId())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .budget(c.getBudget())
                .commissionRate(c.getCommissionRate())
                .paymentDay(c.getPaymentDay())
                .contractPdfUrl(c.getContractPdfUrl())
                .signedPdfUrl(c.getSignedPdfUrl())
                .signedDate(c.getSignedDate())
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
                .freelancerName(getMockUserName(c.getFreelancerId()))
                .employerName(getMockUserName(c.getEmployerId()))
                .build();
    }

    private ContractSummary toSummary(Contract c) {
        return ContractSummary.builder()
                .id(c.getId())
                .contractId(c.getContractId())
                .projectName(c.getProjectName())
                .freelancerId(c.getFreelancerId())
                .employerId(c.getEmployerId())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .budget(c.getBudget())
                .employerSigned(c.getEmployerSignature() != null)
                .freelancerSigned(c.getFreelancerSignature() != null)
                .freelancerName(getMockUserName(c.getFreelancerId()))
                .employerName(getMockUserName(c.getEmployerId()))
                .build();
    }
}
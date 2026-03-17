package com.fallguys.contract.service;

import com.fallguys.common.ai.port.ContractEngine;
import com.fallguys.common.event.ContractAIAnalysisRequestedEvent;
import com.fallguys.contract.entity.Contract;
import com.fallguys.contract.repository.ContractRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContractAiEventListener {

    private final ContractEngine contractEngine;
    private final ObjectMapper objectMapper;
    private final ContractRepository contractRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleContractAIAnalysisRequestedEvent(ContractAIAnalysisRequestedEvent event) {
        log.info("비동기 AI 계약서 분석 시작 - contractId: {}", event.contractId());
        Instant startedAt = Instant.now();
        try {
            Contract contract = contractRepository.findById(event.contractId())
                    .orElseThrow(() -> new IllegalArgumentException("Contract not found for id: " + event.contractId()));

            byte[] pdfBytes = event.pdfBytes();
            if (pdfBytes == null || pdfBytes.length == 0) {
                throw new IllegalArgumentException("Missing PDF bytes for contractId: " + event.contractId());
            }

            log.info(
                    "계약 AI 분석을 시작합니다. contractId={}, externalContractId={}, pdfBytes={}",
                    event.contractId(),
                    contract.getContractId(),
                    pdfBytes.length
            );
            String aiResultJson = contractEngine.analyzeContract(pdfBytes, "contract_" + contract.getContractId() + ".pdf");
            log.info(
                    "계약 AI 분석 응답을 수신했습니다. contractId={}, externalContractId={}, responseLength={}, elapsedMs={}",
                    event.contractId(),
                    contract.getContractId(),
                    aiResultJson != null ? aiResultJson.length() : 0,
                    Duration.between(startedAt, Instant.now()).toMillis()
            );
            
            JsonNode root = objectMapper.readTree(aiResultJson);
            StringBuilder adviceBuilder = new StringBuilder();
            
            if (root.has("summary")) {
                adviceBuilder.append("### 📄 계약서 요약\n").append(root.get("summary").asText()).append("\n\n");
            }
            if (root.has("toxic_clauses") && root.get("toxic_clauses").isArray() && root.get("toxic_clauses").size() > 0) {
                adviceBuilder.append("### ⚠️ 주의 / 독소 조항\n");
                for (JsonNode clause : root.get("toxic_clauses")) {
                    String text = clause.isTextual() ? clause.asText() : (clause.has("text") ? clause.get("text").asText() : clause.toString());
                    adviceBuilder.append("- ").append(text).append("\n");
                }
                adviceBuilder.append("\n");
            }
            if (root.has("recommendations") && root.get("recommendations").isArray() && root.get("recommendations").size() > 0) {
                adviceBuilder.append("### 💡 권장 사항\n");
                for (JsonNode rec : root.get("recommendations")) {
                    String text = rec.isTextual() ? rec.asText() : (rec.has("text") ? rec.get("text").asText() : rec.toString());
                    adviceBuilder.append("- ").append(text).append("\n");
                }
            }
            String finalAdvice = adviceBuilder.toString().trim();
            if (finalAdvice.isEmpty()) {
                finalAdvice = "AI 분석 내용이 없습니다.";
            }

            saveAiLegalAdvice(event.contractId(), finalAdvice);
            log.info(
                    "계약 AI 분석이 완료되었습니다. contractId={}, elapsedMs={}",
                    event.contractId(),
                    Duration.between(startedAt, Instant.now()).toMillis()
            );
            log.info("비동기 AI 계약서 분석 완료 및 저장 - contractId: {}", event.contractId());
        } catch (Exception e) {
            log.error("AI 계약서 분석 실패 - contractId: {}", event.contractId(), e);
            try {
                saveAiLegalAdvice(event.contractId(), "AI 분석 중 오류가 발생했습니다.");
            } catch (Exception innerE) {
                log.error("계약 AI 오류 메시지 저장에 실패했습니다. contractId={}", event.contractId(), innerE);
            }
        }
    }

    @Transactional
    protected void saveAiLegalAdvice(Long contractId, String advice) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found for id: " + contractId));
        contract.setAiLegalAdvice(advice);
        contractRepository.save(contract);
        log.info(
                "계약 AI 자문을 저장했습니다. contractId={}, adviceLength={}",
                contractId,
                advice != null ? advice.length() : 0
        );
    }
}

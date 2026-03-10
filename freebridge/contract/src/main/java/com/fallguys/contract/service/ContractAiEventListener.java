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
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

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
        try {
            Contract contract = contractRepository.findById(event.contractId())
                    .orElseThrow(() -> new IllegalArgumentException("Contract not found for id: " + event.contractId()));

            byte[] pdfBytes = event.pdfBytes();
            if (pdfBytes == null || pdfBytes.length == 0) {
                throw new IllegalArgumentException("Missing PDF bytes for contractId: " + event.contractId());
            }

            String aiResultJson = contractEngine.analyzeContract(pdfBytes, "contract_" + contract.getContractId() + ".pdf");
            
            JsonNode root = objectMapper.readTree(aiResultJson);
            StringBuilder adviceBuilder = new StringBuilder();
            
            if (root.has("summary")) {
                adviceBuilder.append("### 📄 계약서 요약\n").append(root.get("summary").asText()).append("\n\n");
            }
            if (root.has("toxic_clauses") && root.get("toxic_clauses").isArray() && root.get("toxic_clauses").size() > 0) {
                adviceBuilder.append("### ⚠️ 주의 / 독소 조항\n");
                for (JsonNode clause : root.get("toxic_clauses")) {
                    adviceBuilder.append("- ").append(clause.asText()).append("\n");
                }
                adviceBuilder.append("\n");
            }
            if (root.has("recommendations") && root.get("recommendations").isArray() && root.get("recommendations").size() > 0) {
                adviceBuilder.append("### 💡 권장 사항\n");
                for (JsonNode rec : root.get("recommendations")) {
                    adviceBuilder.append("- ").append(rec.asText()).append("\n");
                }
            }
            String finalAdvice = adviceBuilder.toString().trim();
            if (finalAdvice.isEmpty()) {
                finalAdvice = "AI 분석 내용이 없습니다.";
            }
            
            contract.setAiLegalAdvice(finalAdvice);
            contractRepository.save(contract);
            log.info("비동기 AI 계약서 분석 완료 및 저장 - contractId: {}", event.contractId());
        } catch (Exception e) {
            log.error("AI 계약서 분석 실패 - contractId: {}", event.contractId(), e);
            contractRepository.findById(event.contractId()).ifPresent(contract -> {
                contract.setAiLegalAdvice("AI 분석 중 오류가 발생했습니다.");
                contractRepository.save(contract);
            });
        }
    }
}

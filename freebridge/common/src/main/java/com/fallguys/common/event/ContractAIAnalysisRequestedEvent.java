package com.fallguys.common.event;

public record ContractAIAnalysisRequestedEvent(Long contractId, byte[] pdfBytes) {
}

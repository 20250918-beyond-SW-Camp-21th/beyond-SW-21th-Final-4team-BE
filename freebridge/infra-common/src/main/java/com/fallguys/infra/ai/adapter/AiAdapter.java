package com.fallguys.infra.ai.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fallguys.common.ai.dto.FreelancerAiReputationReportDto;
import com.fallguys.common.ai.port.ChatEngine;
import com.fallguys.common.ai.port.ContractEngine;
import com.fallguys.common.ai.port.RecommendationEngine;
import com.fallguys.common.ai.port.ReviewEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.MediaType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class AiAdapter implements ChatEngine, ContractEngine, RecommendationEngine, ReviewEngine {

    private static final int SYNC_MAX_ATTEMPTS = 3;
    private static final long SYNC_INITIAL_BACKOFF_MS = 500L;
    private static final int MAX_LOG_BODY_LENGTH = 300;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Executor taskExecutor;

    @Value("${fallguys.ai.python-url}")
    private String pythonUrl;

    public AiAdapter(ObjectMapper objectMapper,
                     @Qualifier("taskExecutor") Executor taskExecutor) {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) java.time.Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) java.time.Duration.ofSeconds(90).toMillis());
        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public String askChatBot(String question, Map<String, Object> context) {
        return restClient.post()
                .uri(pythonUrl + "/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("question", question, "context", context))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new AiServiceException("AI 채팅 서비스가 오류 응답을 반환했습니다.");
                })
                .body(String.class);
    }

    @Override
    public String generateContract(Map<String, Object> agreementData) {
        return restClient.post()
                .uri(pythonUrl + "/ai/contract")
                .contentType(MediaType.APPLICATION_JSON)
                .body(agreementData)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new AiServiceException("AI 계약 서비스가 오류 응답을 반환했습니다.");
                })
                .body(String.class);
    }

    @Override
    public String analyzeContract(byte[] pdfBytes, String filename) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("pdfBytes must not be null or empty");
        }
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("filename must not be null or blank");
        }
        if (!filename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("filename must have .pdf extension");
        }
        
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new ByteArrayResource(pdfBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        }, MediaType.APPLICATION_PDF);
        
        Instant requestedAt = Instant.now();
        log.info(
                "Python 계약 분석 API를 호출합니다. filename={}, pdfBytes={}, pythonUrl={}",
                filename,
                pdfBytes.length,
                pythonUrl
        );

        String responseBody = restClient.post()
                .uri(pythonUrl + "/api/v1/analysis/contract")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(builder.build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    String errorBody = new String(response.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    log.warn(
                            "AI 계약서 분석 요청 실패. status={}, body={}",
                            response.getStatusCode(),
                            truncateForLog(errorBody)
                    );
                    throw new AiServiceException("AI 계약서 분석 요청이 실패했습니다: " + response.getStatusCode());
                })
                .body(String.class);
        log.info(
                "Python 계약 분석 API 호출이 완료되었습니다. filename={}, responseLength={}, elapsedMs={}",
                filename,
                responseBody != null ? responseBody.length() : 0,
                Duration.between(requestedAt, Instant.now()).toMillis()
        );
        return responseBody;
    }

    @Override
    public <T> List<T> recommend(String type, Long id, Class<T> responseType) {
        try {
            String rawJson = restClient.get()
                    .uri(pythonUrl + "/ai/recommend/{type}/{id}", type, id)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new AiServiceException("AI 추천 서비스가 오류 응답을 반환했습니다.");
                    })
                    .body(String.class);

            return objectMapper.readValue(
                    rawJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, responseType)
            );
        } catch (JsonProcessingException e) {
            throw new AiServiceException("AI 응답 파싱에 실패했습니다.", e);
        }
    }

    @Override
    public Map<String, Object> analyzeReputation(List<Integer> scores, List<String> reviews) {
        try {
            String rawJson = restClient.post()
                    .uri(pythonUrl + "/ai/analyze-reputation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("scores", scores, "reviews", reviews))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new AiServiceException("AI 평판 분석 서비스가 오류 응답을 반환했습니다.");
                    })
                    .body(String.class);

            return objectMapper.readValue(
                    rawJson,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
            );
        } catch (JsonProcessingException e) {
            throw new AiServiceException("AI 분석 응답 파싱에 실패했습니다.", e);
        }
    }

    @Override
    public <T> List<T> recommendFreelancers(Long jobId, String title, String description, String skills, Class<T> responseType) {
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "jobId", jobId,
                    "title", title,
                    "description", description,
                    "skills", skills == null ? "" : skills
            ));

            String rawJson = restClient.post()
                    .uri(pythonUrl + "/api/v1/employer/recommendations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        String errorBody = new String(response.getBody().readAllBytes());
                        log.warn(
                                "AI 추천 요청이 실패했습니다. status={}, body={}",
                                response.getStatusCode(),
                                truncateForLog(errorBody)
                        );
                        throw new AiServiceException(
                                "AI 추천 요청에 실패했습니다: " + response.getStatusCode()
                        );
                    })
                    .body(String.class);

            if (rawJson == null || rawJson.trim().isEmpty()) {
                throw new AiServiceException("AI 추천 서비스가 빈 응답을 반환했습니다.");
            }

            JsonNode root = objectMapper.readTree(rawJson);
            if (root == null || !root.has("data") || !root.get("data").isArray()) {
                log.warn("AI recommendation service returned an invalid payload. body={}", truncateForLog(rawJson));
                throw new AiServiceException("AI 추천 서비스가 올바르지 않은 응답 형식을 반환했습니다.");
            }

            return objectMapper.readValue(
                    root.get("data").toString(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, responseType)
            );
        } catch (JsonProcessingException e) {
            throw new AiServiceException("AI 추천 응답 파싱에 실패했습니다.", e);
        }
    }

    @Override
    public <T> List<T> recommendJobs(Long freelancerId, String skills, String experience, Class<T> responseType) {
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "freelancerId", freelancerId,
                    "skills", skills,
                    "experience", experience
            ));

            String rawJson = restClient.post()
                    .uri(pythonUrl + "/api/v1/freelancer/recommendations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new AiServiceException("AI 프리랜서 추천 서비스가 오류 응답을 반환했습니다.");
                    })
                    .body(String.class);

            if (rawJson == null || rawJson.trim().isEmpty()) {
                throw new AiServiceException("AI 추천 서비스가 빈 응답을 반환했습니다.");
            }

            JsonNode root = objectMapper.readTree(rawJson);
            if (root == null || !root.has("data") || !root.get("data").isArray()) {
                throw new AiServiceException("AI 추천 서비스가 올바르지 않은 응답 형식을 반환했습니다.");
            }

            return objectMapper.readValue(
                    root.get("data").toString(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, responseType)
            );
        } catch (JsonProcessingException e) {
            throw new AiServiceException("AI 추천 응답 파싱에 실패했습니다.", e);
        }
    }

    @Override
    public void syncToAiServer(Long id, Long refId, String type, String content, String status) {
        CompletableFuture.runAsync(() -> runWithRetry(
                "AI sync",
                () -> restClient.post()
                        .uri(pythonUrl + "/api/v1/sync/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "id", id,
                                "refId", refId,
                                "type", type,
                                "content", content,
                                "status", status
                        ))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, (request, response) -> {
                            throw new AiServiceException("AI 동기화 서비스가 오류 응답을 반환했습니다: " + response.getStatusCode());
                        })
                        .toBodilessEntity(),
                Map.of("id", id, "refId", refId, "type", type)
        ), taskExecutor);
    }

    @Override
    public void syncProjectExperience(Long documentId, Long refId, String content, String status) {
        CompletableFuture.runAsync(() -> runWithRetry(
                "AI review sync",
                () -> restClient.post()
                        .uri(pythonUrl + "/api/v1/sync/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "id", documentId,
                                "type", "experience",
                                "content", content,
                                "status", status,
                                "refId", refId
                        ))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, (req, res) -> {
                            throw new AiServiceException("AI 리뷰 동기화 서비스가 오류 응답을 반환했습니다.");
                        })
                        .toBodilessEntity(),
                Map.of("id", documentId, "refId", refId, "type", "experience")
        ), taskExecutor);
    }

    public void syncProjectExperience(AiSyncRequest request) {
        CompletableFuture.runAsync(() -> runWithRetry(
                "AI review sync",
                () -> restClient.post()
                        .uri(pythonUrl + "/api/v1/sync/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, (req, res) -> {
                            throw new AiServiceException("AI 리뷰 동기화 서비스가 오류 응답을 반환했습니다.");
                        })
                        .toBodilessEntity(),
                Map.of("id", request.id(), "type", "experience")
        ), taskExecutor);
    }

    @Override
    public FreelancerAiReputationReportDto getFreelancerAnalysis(Long freelancerId) {
        try {
            String rawJson = restClient.get()
                    .uri(pythonUrl + "/api/v1/analysis/freelancer/{id}", freelancerId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new AiServiceException("AI 분석 서비스가 오류 응답을 반환했습니다.");
                    })
                    .body(String.class);

            return objectMapper.readValue(rawJson, FreelancerAiReputationReportDto.class);
        } catch (RestClientException e) {
            log.error("AI 분석 서비스 호출에 실패했습니다. freelancerId={}", freelancerId, e);
            throw new AiServiceException("AI 분석 서비스 호출에 실패했습니다.", e);
        } catch (JsonProcessingException e) {
            log.error("AI 분석 응답 파싱에 실패했습니다. freelancerId={}", freelancerId, e);
            throw new AiServiceException("AI 분석 응답 파싱에 실패했습니다.", e);
        } catch (RuntimeException e) {
            log.error("예상치 못한 AI 분석 오류가 발생했습니다. freelancerId={}", freelancerId, e);
            if (e instanceof AiServiceException) {
                throw e;
            }
            throw new AiServiceException("예상치 못한 AI 분석 오류가 발생했습니다.", e);
        }
    }

    private void runWithRetry(String operation, Runnable action, Map<String, Object> metadata) {
        long backoffMs = SYNC_INITIAL_BACKOFF_MS;
        for (int attempt = 1; attempt <= SYNC_MAX_ATTEMPTS; attempt++) {
            try {
                action.run();
                log.info("{} 성공: {}", operation, metadata);
                return;
            } catch (Exception e) {
                if (attempt == SYNC_MAX_ATTEMPTS) {
                    log.error("{} 재시도 후에도 실패했습니다: {}", operation, metadata, e);
                    return;
                }

                log.warn("{} 실패, 재시도합니다 (attempt {}/{}): {}", operation, attempt, SYNC_MAX_ATTEMPTS, metadata, e);
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    log.error("{} 재시도 대기 중 인터럽트가 발생했습니다: {}", operation, metadata, interruptedException);
                    return;
                }
                backoffMs *= 2;
            }
        }
    }

    private String truncateForLog(String body) {
        if (body == null) {
            return "";
        }
        if (body.length() <= MAX_LOG_BODY_LENGTH) {
            return body;
        }
        return body.substring(0, MAX_LOG_BODY_LENGTH) + "...";
    }
}

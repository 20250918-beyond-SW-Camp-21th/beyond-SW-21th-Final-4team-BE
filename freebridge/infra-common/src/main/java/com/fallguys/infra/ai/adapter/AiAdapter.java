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
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class AiAdapter implements ChatEngine, ContractEngine, RecommendationEngine, ReviewEngine {

    private static final int SYNC_MAX_ATTEMPTS = 3;
    private static final long SYNC_INITIAL_BACKOFF_MS = 500L;

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
        factory.setReadTimeout((int) java.time.Duration.ofSeconds(30).toMillis());
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
                    throw new AiServiceException("AI chat service returned an error response.");
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
                    throw new AiServiceException("AI contract service returned an error response.");
                })
                .body(String.class);
    }

    @Override
    public <T> List<T> recommend(String type, Long id, Class<T> responseType) {
        try {
            String rawJson = restClient.get()
                    .uri(pythonUrl + "/ai/recommend/{type}/{id}", type, id)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new AiServiceException("AI recommendation service returned an error response.");
                    })
                    .body(String.class);

            return objectMapper.readValue(
                    rawJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, responseType)
            );
        } catch (JsonProcessingException e) {
            throw new AiServiceException("Failed to parse AI response.", e);
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
                        throw new AiServiceException("AI reputation analysis service returned an error response.");
                    })
                    .body(String.class);

            return objectMapper.readValue(
                    rawJson,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
            );
        } catch (JsonProcessingException e) {
            throw new AiServiceException("Failed to parse AI analysis response.", e);
        }
    }

    @Override
    public <T> List<T> recommendFreelancers(Long jobId, String title, String description, Class<T> responseType) {
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "jobId", jobId,
                    "title", title,
                    "description", description
            ));

            String rawJson = restClient.post()
                    .uri(pythonUrl + "/api/v1/employer/recommendations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        String errorBody = new String(response.getBody().readAllBytes());
                        throw new AiServiceException(
                                "AI recommendation request failed: " + response.getStatusCode() + " - " + errorBody
                        );
                    })
                    .body(String.class);

            if (rawJson == null || rawJson.trim().isEmpty()) {
                throw new AiServiceException("AI recommendation service returned an empty response.");
            }

            JsonNode root = objectMapper.readTree(rawJson);
            if (root == null || !root.has("data") || !root.get("data").isArray()) {
                throw new AiServiceException("AI recommendation service returned an invalid payload: " + rawJson);
            }

            return objectMapper.readValue(
                    root.get("data").toString(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, responseType)
            );
        } catch (JsonProcessingException e) {
            throw new AiServiceException("Failed to parse AI recommendation response.", e);
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
                        throw new AiServiceException("AI freelancer recommendation service returned an error response.");
                    })
                    .body(String.class);

            if (rawJson == null || rawJson.trim().isEmpty()) {
                throw new AiServiceException("AI recommendation service returned an empty response.");
            }

            JsonNode root = objectMapper.readTree(rawJson);
            if (root == null || !root.has("data") || !root.get("data").isArray()) {
                throw new AiServiceException("AI recommendation service returned an invalid payload.");
            }

            return objectMapper.readValue(
                    root.get("data").toString(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, responseType)
            );
        } catch (JsonProcessingException e) {
            throw new AiServiceException("Failed to parse AI recommendation response.", e);
        }
    }

    @Override
    public void syncToAiServer(Long id, String type, String content, String status) {
        CompletableFuture.runAsync(() -> runWithRetry(
                "AI sync",
                () -> restClient.post()
                        .uri(pythonUrl + "/api/v1/sync/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "id", id,
                                "type", type,
                                "content", content,
                                "status", status
                        ))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, (request, response) -> {
                            throw new AiServiceException("AI sync service returned an error response: " + response.getStatusCode());
                        })
                        .toBodilessEntity(),
                Map.of("id", id, "type", type)
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
                            throw new AiServiceException("AI review sync service returned an error response.");
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
                        throw new AiServiceException("AI analysis service returned an error response.");
                    })
                    .body(String.class);

            return objectMapper.readValue(rawJson, FreelancerAiReputationReportDto.class);
        } catch (RestClientException e) {
            log.error("AI analysis service call failed: freelancerId={}", freelancerId, e);
            throw new AiServiceException("AI analysis service call failed.", e);
        } catch (JsonProcessingException e) {
            log.error("AI analysis response parsing failed: freelancerId={}", freelancerId, e);
            throw new AiServiceException("AI analysis response parsing failed.", e);
        } catch (RuntimeException e) {
            log.error("Unexpected AI analysis failure: freelancerId={}", freelancerId, e);
            if (e instanceof AiServiceException) {
                throw e;
            }
            throw new AiServiceException("Unexpected AI analysis failure.", e);
        }
    }

    private void runWithRetry(String operation, Runnable action, Map<String, Object> metadata) {
        long backoffMs = SYNC_INITIAL_BACKOFF_MS;
        for (int attempt = 1; attempt <= SYNC_MAX_ATTEMPTS; attempt++) {
            try {
                action.run();
                log.info("{} succeeded: {}", operation, metadata);
                return;
            } catch (Exception e) {
                if (attempt == SYNC_MAX_ATTEMPTS) {
                    log.error("{} failed after retries: {}", operation, metadata, e);
                    return;
                }

                log.warn("{} failed, retrying (attempt {}/{}): {}", operation, attempt, SYNC_MAX_ATTEMPTS, metadata, e);
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    log.error("{} interrupted during backoff: {}", operation, metadata, interruptedException);
                    return;
                }
                backoffMs *= 2;
            }
        }
    }
}

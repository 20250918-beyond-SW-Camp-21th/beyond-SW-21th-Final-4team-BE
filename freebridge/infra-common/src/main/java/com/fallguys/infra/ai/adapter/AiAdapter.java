package com.fallguys.infra.ai.adapter;

import com.fallguys.common.ai.port.ChatEngine;
import com.fallguys.common.ai.port.ContractEngine;
import com.fallguys.common.ai.port.RecommendationEngine;
import com.fallguys.common.ai.port.ReviewEngine;
import com.fallguys.common.ai.dto.FreelancerAiReputationReportDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class AiAdapter implements ChatEngine, ContractEngine, RecommendationEngine, ReviewEngine {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Executor taskExecutor; // [코드래빗 피드백] AsyncConfig의 taskExecutor 주입

    @Value("${fallguys.ai.python-url}")
    private String pythonUrl;

    public AiAdapter(ObjectMapper objectMapper,
                     @Qualifier("taskExecutor") Executor taskExecutor) {
        // Create a dedicated RestClient to avoid JdkHttpClient POST body-drop bugs
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
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
                    throw new RuntimeException("AI Chat 서비스 응답 오류");
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
                    throw new RuntimeException("AI 계약서 생성 서비스 응답 오류");
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
                        throw new RuntimeException("AI 추천 서비스 연결 실패");
                    })
                    .body(String.class);

            return objectMapper.readValue(rawJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, responseType));

        } catch (JsonProcessingException e) {
            throw new RuntimeException("AI 응답 데이터 파싱 실패", e);
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
                        throw new RuntimeException("AI 리뷰 분석 서비스 응답 오류");
                    })
                    .body(String.class);

            return objectMapper.readValue(rawJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("AI 분석 데이터 파싱 실패", e);
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
                        throw new RuntimeException("AI 추천 서버 통신 오류: " + response.getStatusCode() + " - " + errorBody);
                    })
                    .body(String.class);

            if (rawJson == null || rawJson.trim().isEmpty()) {
                throw new RuntimeException("AI 추천 서버로부터 빈 응답(Null)을 수신했습니다.");
            }

            JsonNode root = objectMapper.readTree(rawJson);

            if (root == null || !root.has("data") || !root.get("data").isArray()) {
                throw new RuntimeException("AI 서버로부터 잘못된 응답 형식을 수신했습니다. 수신 데이터: " + rawJson);
            }

            JsonNode dataNode = root.get("data");

            return objectMapper.readValue(dataNode.toString(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, responseType));

        } catch (JsonProcessingException e) {
            throw new RuntimeException("AI 응답 데이터 파싱 실패", e);
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
                        throw new RuntimeException("AI 프리랜서 맞춤 추천 서비스 응답 오류");
                    })
                    .body(String.class);

            if (rawJson == null || rawJson.trim().isEmpty()) {
                throw new RuntimeException("AI 추천 서버로부터 빈 응답(Null)을 수신했습니다.");
            }

            JsonNode root = objectMapper.readTree(rawJson);

            if (root == null || !root.has("data") || !root.get("data").isArray()) {
                throw new RuntimeException("AI 서버로부터 잘못된 응답 형식을 수신했습니다.");
            }

            return objectMapper.readValue(root.get("data").toString(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, responseType));

        } catch (JsonProcessingException e) {
            throw new RuntimeException("AI 응답 데이터 파싱 실패", e);
        }
    }

    @Override
    public void syncToAiServer(Long id, String type, String content, String status) {
        CompletableFuture.runAsync(() -> {
            try {
                restClient.post()
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
                            throw new RuntimeException("AI 서버 응답 오류: " + response.getStatusCode());
                        })
                        .toBodilessEntity();
                log.info("AI 서버 실시간 동기화 성공: id={}, type={}", id, type);
            } catch (Exception e) {
                log.error("AI 서버 실시간 동기화 실패: id={}, type={}", id, type, e);
            }
        }, taskExecutor);
    }

    public void syncProjectExperience(AiSyncRequest request) {
        CompletableFuture.runAsync(() -> {
            try {
                restClient.post()
                        .uri(pythonUrl + "/api/v1/sync/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, (req, res) -> {
                            throw new RuntimeException("AI 동기화 실패");
                        })
                        .toBodilessEntity();
                log.info("AI 리뷰 데이터 동기화 성공: id={}", request.id());
            } catch (Exception e) {
                log.error("AI 리뷰 데이터 동기화 실패", e);
            }
        }, taskExecutor);
    }

    @Override
    public FreelancerAiReputationReportDto getFreelancerAnalysis(Long freelancerId) {
        try {
            String rawJson = restClient.get()
                    .uri(pythonUrl + "/api/v1/analysis/freelancer/{id}", freelancerId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new RuntimeException("AI 분석 서비스 응답 오류");
                    })
                    .body(String.class);

            return objectMapper.readValue(rawJson, FreelancerAiReputationReportDto.class);
        } catch (JsonProcessingException e) {
            log.error("AI 분석 데이터 파싱 실패: freelancerId={}", freelancerId, e);
            throw new RuntimeException("AI 분석 결과 해석 중 오류 발생");
        }
    }
}

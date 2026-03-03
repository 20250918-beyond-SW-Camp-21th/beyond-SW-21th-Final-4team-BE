package com.fallguys.infra.ai.adapter;

import com.fallguys.common.ai.port.ChatEngine;
import com.fallguys.common.ai.port.ContractEngine;
import com.fallguys.common.ai.port.RecommendationEngine;
import com.fallguys.common.ai.port.ReviewEngine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AiAdapter implements ChatEngine, ContractEngine, RecommendationEngine, ReviewEngine {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${fallguys.ai.python-url}")
    private String pythonUrl;

    @Override
    public String askChatBot(String question, Map<String, Object> context) {
        return restClient.post()
                .uri(pythonUrl + "/ai/chat")
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
            // 1. Raw JSON String으로 가져오기
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

    public <T> List<T> recommendFreelancers(Long jobId, String title, String description, Class<T> responseType) {
        try {
            // FastAPI의 /api/v1/employer/recommendations 엔드포인트로 POST 요청
            String rawJson = restClient.post()
                    .uri(pythonUrl + "/api/v1/employer/recommendations")
                    .body(Map.of(
                            "jobId", jobId,
                            "title", title,
                            "description", description
                    ))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new RuntimeException("AI 추천 서버 통신 오류");
                    })
                    .body(String.class);

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
}
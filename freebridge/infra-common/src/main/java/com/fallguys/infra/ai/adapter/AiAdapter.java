package com.fallguys.infra.ai.adapter;

import com.fallguys.common.ai.dto.FreelancerAiReputationReportDto;
import com.fallguys.common.ai.port.ChatEngine;
import com.fallguys.common.ai.port.ContractEngine;
import com.fallguys.common.ai.port.RecommendationEngine;
import com.fallguys.common.ai.port.ReviewEngine;
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
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class AiAdapter implements ChatEngine, ContractEngine, RecommendationEngine, ReviewEngine {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Executor taskExecutor; // [肄붾뱶?섎퉿 ?쇰뱶諛? AsyncConfig??taskExecutor 二쇱엯

    @Value("${fallguys.ai.python-url}")
    private String pythonUrl;

    public AiAdapter(ObjectMapper objectMapper,
                     @Qualifier("taskExecutor") Executor taskExecutor) {
        // Create a dedicated RestClient to avoid JdkHttpClient POST body-drop bugs
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
                    throw new RuntimeException("AI Chat ?쒕퉬???묐떟 ?ㅻ쪟");
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
                    throw new RuntimeException("AI 怨꾩빟???앹꽦 ?쒕퉬???묐떟 ?ㅻ쪟");
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
                        throw new RuntimeException("AI 異붿쿇 ?쒕퉬???곌껐 ?ㅽ뙣");
                    })
                    .body(String.class);

            return objectMapper.readValue(
                    rawJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, responseType)
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("AI ?묐떟 ?곗씠???뚯떛 ?ㅽ뙣", e);
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
                        throw new RuntimeException("AI 由щ럭 遺꾩꽍 ?쒕퉬???묐떟 ?ㅻ쪟");
                    })
                    .body(String.class);

            return objectMapper.readValue(
                    rawJson,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("AI 遺꾩꽍 ?곗씠???뚯떛 ?ㅽ뙣", e);
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
                        throw new RuntimeException(
                                "AI 異붿쿇 ?쒕쾭 ?듭떊 ?ㅻ쪟: " + response.getStatusCode() + " - " + errorBody
                        );
                    })
                    .body(String.class);

            if (rawJson == null || rawJson.trim().isEmpty()) {
                throw new RuntimeException("AI 異붿쿇 ?쒕쾭濡쒕???鍮??묐떟(Null)???섏떊?덉뒿?덈떎.");
            }

            JsonNode root = objectMapper.readTree(rawJson);
            if (root == null || !root.has("data") || !root.get("data").isArray()) {
                throw new RuntimeException("AI ?쒕쾭濡쒕????섎せ???묐떟 ?뺤떇???섏떊?덉뒿?덈떎. ?섏떊 ?곗씠?? " + rawJson);
            }

            JsonNode dataNode = root.get("data");
            return objectMapper.readValue(
                    dataNode.toString(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, responseType)
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("AI ?묐떟 ?곗씠???뚯떛 ?ㅽ뙣", e);
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
                        throw new RuntimeException("AI ?꾨━?쒖꽌 留욎땄 異붿쿇 ?쒕퉬???묐떟 ?ㅻ쪟");
                    })
                    .body(String.class);

            if (rawJson == null || rawJson.trim().isEmpty()) {
                throw new RuntimeException("AI 異붿쿇 ?쒕쾭濡쒕???鍮??묐떟(Null)???섏떊?덉뒿?덈떎.");
            }

            JsonNode root = objectMapper.readTree(rawJson);
            if (root == null || !root.has("data") || !root.get("data").isArray()) {
                throw new RuntimeException("AI ?쒕쾭濡쒕????섎せ???묐떟 ?뺤떇???섏떊?덉뒿?덈떎.");
            }

            return objectMapper.readValue(
                    root.get("data").toString(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, responseType)
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("AI ?묐떟 ?곗씠???뚯떛 ?ㅽ뙣", e);
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
                            throw new RuntimeException("AI ?쒕쾭 ?묐떟 ?ㅻ쪟: " + response.getStatusCode());
                        })
                        .toBodilessEntity();
                log.info("AI ?쒕쾭 ?ㅼ떆媛??숆린???깃났: id={}, type={}", id, type);
            } catch (Exception e) {
                log.error("AI ?쒕쾭 ?ㅼ떆媛??숆린???ㅽ뙣: id={}, type={}", id, type, e);
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
                            throw new RuntimeException("AI ?숆린???ㅽ뙣");
                        })
                        .toBodilessEntity();
                log.info("AI 由щ럭 ?곗씠???숆린???깃났: id={}", request.id());
            } catch (Exception e) {
                log.error("AI 由щ럭 ?곗씠???숆린???ㅽ뙣", e);
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
                        throw new RuntimeException("AI analysis service returned an error response");
                    })
                    .body(String.class);

            return objectMapper.readValue(rawJson, FreelancerAiReputationReportDto.class);
        } catch (RestClientException e) {
            log.error("AI analysis service call failed: freelancerId={}", freelancerId, e);
            return emptyFreelancerAnalysisReport();
        } catch (JsonProcessingException e) {
            log.error("AI analysis response parsing failed: freelancerId={}", freelancerId, e);
            return emptyFreelancerAnalysisReport();
        } catch (RuntimeException e) {
            log.error("Unexpected AI analysis failure: freelancerId={}", freelancerId, e);
            return emptyFreelancerAnalysisReport();
        }
    }

    private FreelancerAiReputationReportDto emptyFreelancerAnalysisReport() {
        return new FreelancerAiReputationReportDto(
                "AI 분석 결과를 아직 불러오지 못했습니다.",
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}

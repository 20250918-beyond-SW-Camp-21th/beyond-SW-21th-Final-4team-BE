package com.fallguys.infra.ai.adapter;

import com.fallguys.common.ai.port.ChatEngine;
import com.fallguys.common.ai.port.ContractEngine;
import com.fallguys.common.ai.port.RecommendationEngine;
import com.fallguys.common.ai.port.ReviewEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AiAdapter implements ChatEngine, ContractEngine, RecommendationEngine, ReviewEngine {

    private final RestClient restClient;

    @Value("${fallguys.ai.python-url}")
    private String pythonUrl;

    @Override
    public String askChatBot(String question, Map<String, Object> context) {
        return restClient.post()
                .uri(pythonUrl + "/ai/chat")
                .body(Map.of("question", question, "context", context))
                .retrieve()
                .body(String.class);
    }

    @Override
    public String generateContract(Map<String, Object> agreementData) {
        return restClient.post()
                .uri(pythonUrl + "/ai/contract")
                .body(agreementData)
                .retrieve()
                .body(String.class);
    }

    @Override
    public <T> List<T> recommend(String type, Long id, Class<T> responseType) {
        return restClient.get()
                .uri(pythonUrl + "/ai/recommend/{type}/{id}", type, id)
                .retrieve()
                .body(new ParameterizedTypeReference<List<T>>() {});
    }

    @Override
    public Map<String, Object> analyzeReputation(List<Integer> scores, List<String> reviews) {
        return restClient.post()
                .uri(pythonUrl + "/ai/analyze-reputation")
                .body(Map.of("scores", scores, "reviews", reviews))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }
}
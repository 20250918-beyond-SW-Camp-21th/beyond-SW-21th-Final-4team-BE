package com.fallguys.common.ai.port;

import java.util.List;

public interface RecommendationEngine {
    <T> List<T> recommend(String type, Long id, Class<T> responseType);
}

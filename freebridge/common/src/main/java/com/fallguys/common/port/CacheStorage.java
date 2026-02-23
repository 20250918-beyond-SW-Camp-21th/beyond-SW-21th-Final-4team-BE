package com.fallguys.common.port;

import java.time.Duration;
import java.util.Optional;

public interface CacheStorage {
    void set(String key, Object value, Duration timeout);
    <T> Optional<T> get(String key, Class<T> type);
    void delete(String key);
    boolean hasKey(String key);
}
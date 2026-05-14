package com.example.dati_backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RedisJsonCacheService {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> Optional<T> get(String key, TypeReference<T> typeReference) {
        String value;
        try {
            value = redisTemplate.opsForValue().get(key);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(objectMapper.readValue(value, typeReference));
        } catch (JsonProcessingException exception) {
            redisTemplate.delete(key);
            return Optional.empty();
        }
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to serialize cache value");
        } catch (RuntimeException exception) {
            // Redis is an acceleration layer; MySQL remains the source of truth.
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException exception) {
            // Ignore cache deletion failures.
        }
    }

    public void deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (RuntimeException exception) {
            // Ignore cache deletion failures.
        }
    }
}

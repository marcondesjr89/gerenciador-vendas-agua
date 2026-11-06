package com.distribuidora.agua.platform.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResilientCacheErrorHandlerTest {

    private final ResilientCacheErrorHandler errorHandler = new ResilientCacheErrorHandler();

    @Mock
    private Cache cache;

    @Test
    @DisplayName("Should handle cache GET error gracefully without throwing exception")
    void shouldHandleCacheGetErrorGracefully() {
        when(cache.getName()).thenReturn("catalog-products");
        RuntimeException redisError = new RuntimeException("Redis connection refused on port 6379");

        assertThatNoException().isThrownBy(() ->
                errorHandler.handleCacheGetError(redisError, cache, "all-products")
        );
    }

    @Test
    @DisplayName("Should handle cache PUT error gracefully without throwing exception")
    void shouldHandleCachePutErrorGracefully() {
        when(cache.getName()).thenReturn("catalog-products");
        RuntimeException redisError = new RuntimeException("Redis cluster timeout");

        assertThatNoException().isThrownBy(() ->
                errorHandler.handleCachePutError(redisError, cache, "key-1", "val-1")
        );
    }

    @Test
    @DisplayName("Should handle cache EVICT error gracefully without throwing exception")
    void shouldHandleCacheEvictErrorGracefully() {
        when(cache.getName()).thenReturn("catalog-products");
        RuntimeException redisError = new RuntimeException("Redis node down");

        assertThatNoException().isThrownBy(() ->
                errorHandler.handleCacheEvictError(redisError, cache, "key-1")
        );
    }

    @Test
    @DisplayName("Should handle cache CLEAR error gracefully without throwing exception")
    void shouldHandleCacheClearErrorGracefully() {
        when(cache.getName()).thenReturn("catalog-products");
        RuntimeException redisError = new RuntimeException("Redis OOM");

        assertThatNoException().isThrownBy(() ->
                errorHandler.handleCacheClearError(redisError, cache)
        );
    }
}

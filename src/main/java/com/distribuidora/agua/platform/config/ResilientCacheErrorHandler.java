package com.distribuidora.agua.platform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

public class ResilientCacheErrorHandler implements CacheErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ResilientCacheErrorHandler.class);

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Redis Cache GET failed for cache [{}] with key [{}]. Falling back gracefully to database: {}",
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Redis Cache PUT failed for cache [{}] with key [{}]: {}",
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Redis Cache EVICT failed for cache [{}] with key [{}]: {}",
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Redis Cache CLEAR failed for cache [{}]: {}",
                cache.getName(), exception.getMessage());
    }
}

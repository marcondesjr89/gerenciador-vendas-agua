package com.distribuidora.agua.platform.feature;

import com.distribuidora.agua.platform.api.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service managing application runtime feature flags without requiring service restart.
 */
@Service
public class FeatureFlagService {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagService.class);

    private final ConcurrentMap<String, FeatureFlagState> flags = new ConcurrentHashMap<>();

    public FeatureFlagService(Environment environment) {
        for (FeatureFlagKey key : FeatureFlagKey.values()) {
            boolean initialValue = Boolean.parseBoolean(
                    environment.getProperty(key.getKey(), String.valueOf(key.isDefaultValue()))
            );
            flags.put(
                    key.getKey(),
                    new FeatureFlagState(
                            key.getKey(),
                            key.getAlias(),
                            initialValue,
                            key.getDefaultDescription(),
                            Instant.now()
                    )
            );
            log.info("Initialized Feature Flag [{}] (alias: [{}]) = {}", key.getKey(), key.getAlias(), initialValue);
        }
    }

    /**
     * Checks if a feature flag is enabled using its strongly typed key.
     */
    public boolean isEnabled(FeatureFlagKey flagKey) {
        if (flagKey == null) {
            return false;
        }
        FeatureFlagState state = flags.get(flagKey.getKey());
        return state != null && state.enabled();
    }

    /**
     * Checks if a feature flag is enabled using either canonical key or alias.
     */
    public boolean isEnabled(String rawKey) {
        return FeatureFlagKey.fromString(rawKey)
                .map(this::isEnabled)
                .orElse(false);
    }

    /**
     * Returns all registered feature flags.
     */
    public List<FeatureFlagState> listAll() {
        return flags.values().stream()
                .sorted(Comparator.comparing(FeatureFlagState::key))
                .toList();
    }

    /**
     * Retrieves the state of a specific feature flag.
     */
    public FeatureFlagState getFlag(String rawKey) {
        FeatureFlagKey key = FeatureFlagKey.fromString(rawKey)
                .orElseThrow(() -> new ResourceNotFoundException("Feature flag nao encontrada: " + rawKey));

        return flags.get(key.getKey());
    }

    /**
     * Dynamically updates the state of a feature flag in runtime.
     */
    public FeatureFlagState updateFlag(String rawKey, boolean enabled) {
        FeatureFlagKey key = FeatureFlagKey.fromString(rawKey)
                .orElseThrow(() -> new ResourceNotFoundException("Feature flag nao encontrada: " + rawKey));

        FeatureFlagState updated = new FeatureFlagState(
                key.getKey(),
                key.getAlias(),
                enabled,
                key.getDefaultDescription(),
                Instant.now()
        );

        flags.put(key.getKey(), updated);
        log.warn("Runtime Feature Flag changed: [{}] is now {}", key.getKey(), enabled ? "ENABLED" : "DISABLED");
        return updated;
    }
}

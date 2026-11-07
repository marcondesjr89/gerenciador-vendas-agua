package com.distribuidora.agua.platform.feature;

import java.time.Instant;

/**
 * Public DTO representing the state of a feature flag.
 */
public record FeatureFlagResponse(
        String key,
        String alias,
        boolean enabled,
        String description,
        Instant updatedAt
) {
    public static FeatureFlagResponse fromState(FeatureFlagState state) {
        return new FeatureFlagResponse(
                state.key(),
                state.alias(),
                state.enabled(),
                state.description(),
                state.updatedAt()
        );
    }
}

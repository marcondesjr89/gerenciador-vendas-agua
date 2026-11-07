package com.distribuidora.agua.platform.feature;

import java.time.Instant;

/**
 * Internal state representation of a feature flag.
 */
public record FeatureFlagState(
        String key,
        String alias,
        boolean enabled,
        String description,
        Instant updatedAt
) {
}

package com.distribuidora.agua.platform.feature;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for toggling or updating a feature flag.
 */
public record UpdateFeatureFlagRequest(
        @NotNull(message = "O estado da feature flag e obrigatorio (true/false)")
        Boolean enabled
) {
}

package com.distribuidora.agua.platform.feature;

import com.distribuidora.agua.platform.api.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FeatureFlagService Unit Tests")
class FeatureFlagServiceTest {

    private MockEnvironment environment;
    private FeatureFlagService featureFlagService;

    @BeforeEach
    void setUp() {
        environment = new MockEnvironment();
        environment.setProperty("feature.fiscal-receipt-preview", "true");
        environment.setProperty("feature.free-delivery-promo", "false");
        featureFlagService = new FeatureFlagService(environment);
    }

    @Test
    @DisplayName("Should load default flags correctly from environment")
    void shouldLoadDefaultFlagsFromEnvironment() {
        assertThat(featureFlagService.isEnabled("feature.fiscal-receipt-preview")).isTrue();
        assertThat(featureFlagService.isEnabled("fiscal-receipt-preview")).isTrue();
        assertThat(featureFlagService.isEnabled(FeatureFlagKey.FISCAL_RECEIPT_PREVIEW)).isTrue();

        assertThat(featureFlagService.isEnabled("feature.free-delivery-promo")).isFalse();
        assertThat(featureFlagService.isEnabled("free-delivery-promo")).isFalse();
        assertThat(featureFlagService.isEnabled(FeatureFlagKey.FREE_DELIVERY_PROMO)).isFalse();
    }

    @Test
    @DisplayName("Should list all registered feature flags")
    void shouldListAllRegisteredFlags() {
        List<FeatureFlagState> flags = featureFlagService.listAll();

        assertThat(flags).hasSize(2);
        assertThat(flags).extracting(FeatureFlagState::key)
                .containsExactlyInAnyOrder("feature.fiscal-receipt-preview", "feature.free-delivery-promo");
    }

    @Test
    @DisplayName("Should update feature flag status in runtime")
    void shouldUpdateFeatureFlagStatusInRuntime() {
        FeatureFlagState updated = featureFlagService.updateFlag("free-delivery-promo", true);

        assertThat(updated.enabled()).isTrue();
        assertThat(featureFlagService.isEnabled("free-delivery-promo")).isTrue();
        assertThat(featureFlagService.isEnabled(FeatureFlagKey.FREE_DELIVERY_PROMO)).isTrue();

        FeatureFlagState disabled = featureFlagService.updateFlag("feature.fiscal-receipt-preview", false);
        assertThat(disabled.enabled()).isFalse();
        assertThat(featureFlagService.isEnabled("fiscal-receipt-preview")).isFalse();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating unknown flag")
    void shouldThrowExceptionWhenFlagNotFound() {
        assertThatThrownBy(() -> featureFlagService.updateFlag("unknown-feature", true))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("unknown-feature");
    }

    @Test
    @DisplayName("Should return false for unknown feature flag query")
    void shouldReturnFalseForUnknownFeatureFlag() {
        assertThat(featureFlagService.isEnabled("non.existent.flag")).isFalse();
    }
}

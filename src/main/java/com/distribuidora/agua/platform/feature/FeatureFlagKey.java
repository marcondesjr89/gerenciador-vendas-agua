package com.distribuidora.agua.platform.feature;

import java.util.Arrays;
import java.util.Optional;

/**
 * Known feature flags supported by the platform.
 */
public enum FeatureFlagKey {
    FISCAL_RECEIPT_PREVIEW(
            "feature.fiscal-receipt-preview",
            "fiscal-receipt-preview",
            "Habilita a geracao e pre-visualizacao estruturada de cupom fiscal em pedidos",
            true
    ),
    FREE_DELIVERY_PROMO(
            "feature.free-delivery-promo",
            "free-delivery-promo",
            "Aplica promocao de frete gratis para entregas qualificadas",
            false
    );

    private final String key;
    private final String alias;
    private final String defaultDescription;
    private final boolean defaultValue;

    FeatureFlagKey(String key, String alias, String defaultDescription, boolean defaultValue) {
        this.key = key;
        this.alias = alias;
        this.defaultDescription = defaultDescription;
        this.defaultValue = defaultValue;
    }

    public String getKey() {
        return key;
    }

    public String getAlias() {
        return alias;
    }

    public String getDefaultDescription() {
        return defaultDescription;
    }

    public boolean isDefaultValue() {
        return defaultValue;
    }

    public static Optional<FeatureFlagKey> fromString(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return Optional.empty();
        }
        String normalized = rawKey.trim().toLowerCase();
        return Arrays.stream(values())
                .filter(k -> k.key.equalsIgnoreCase(normalized) || k.alias.equalsIgnoreCase(normalized))
                .findFirst();
    }
}

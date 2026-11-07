package com.distribuidora.agua.platform.fiscal;

import java.util.Optional;

/**
 * Architectural Port for fiscal receipt printing and preview generation.
 * Decouples order completion from physical or virtual thermal printer implementations.
 */
public interface FiscalReceiptPort {

    /**
     * Generates a structured fiscal receipt / preview for the given order request
     * if the underlying feature flag is enabled.
     *
     * @param request the order details for receipt emission
     * @return an Optional containing the generated FiscalReceipt if enabled, or empty if disabled
     */
    Optional<FiscalReceipt> generateReceipt(FiscalReceiptRequest request);
}

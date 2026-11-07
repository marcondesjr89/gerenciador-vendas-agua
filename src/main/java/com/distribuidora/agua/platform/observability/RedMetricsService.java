package com.distribuidora.agua.platform.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Service providing RED (Rate, Errors, Duration) metrics instrumentation for core business operations.
 */
@Service
public class RedMetricsService {

    private final MeterRegistry meterRegistry;

    public RedMetricsService(Optional<MeterRegistry> meterRegistry) {
        this.meterRegistry = meterRegistry.orElseGet(SimpleMeterRegistry::new);
    }

    /**
     * Increments the total orders created counter with dimensional tags (status, modality).
     */
    public void recordOrderCreated(String status, String modality) {
        Counter.builder("orders.created.total")
                .description("Total number of orders created")
                .tag("status", status != null ? status : "UNKNOWN")
                .tag("modality", modality != null ? modality : "UNKNOWN")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Increments the total order creation errors counter categorized by error type.
     */
    public void recordOrderError(String errorType) {
        Counter.builder("orders.errors.total")
                .description("Total number of order creation errors")
                .tag("error_type", errorType != null ? errorType : "UNKNOWN")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Records the duration taken to process an order creation transaction.
     */
    public void recordOrderCreationDuration(Duration duration) {
        Timer.builder("orders.creation.duration")
                .description("Duration of order creation transaction")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry)
                .record(duration);
    }

    /**
     * Increments the total payments processed counter with dimensional tags (status, method).
     */
    public void recordPaymentProcessed(String status, String method) {
        Counter.builder("payments.processed.total")
                .description("Total number of payments processed")
                .tag("status", status != null ? status : "UNKNOWN")
                .tag("method", method != null ? method : "UNKNOWN")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Records the duration taken to process a payment transaction.
     */
    public void recordPaymentDuration(Duration duration) {
        Timer.builder("payments.processing.duration")
                .description("Duration of payment processing transaction")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry)
                .record(duration);
    }
}

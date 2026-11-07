package com.distribuidora.agua.platform.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedMetricsService Unit Tests")
class RedMetricsServiceTest {

    private MeterRegistry meterRegistry;
    private RedMetricsService redMetricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        redMetricsService = new RedMetricsService(Optional.of(meterRegistry));
    }

    @Test
    @DisplayName("Should increment orders created counter with status and modality tags")
    void shouldIncrementOrdersCreatedCounter() {
        redMetricsService.recordOrderCreated("CONFIRMED", "DELIVERY");

        var counter = meterRegistry.find("orders.created.total")
                .tag("status", "CONFIRMED")
                .tag("modality", "DELIVERY")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should increment orders errors counter with error type tag")
    void shouldIncrementOrdersErrorsCounter() {
        redMetricsService.recordOrderError("INSUFFICIENT_STOCK");

        var counter = meterRegistry.find("orders.errors.total")
                .tag("error_type", "INSUFFICIENT_STOCK")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should record duration for order creation")
    void shouldRecordOrderCreationDuration() {
        redMetricsService.recordOrderCreationDuration(Duration.ofMillis(120));

        var timer = meterRegistry.find("orders.creation.duration").timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(120.0);
    }

    @Test
    @DisplayName("Should increment payments processed counter with status and method tags")
    void shouldIncrementPaymentsProcessedCounter() {
        redMetricsService.recordPaymentProcessed("PAID", "PIX");

        var counter = meterRegistry.find("payments.processed.total")
                .tag("status", "PAID")
                .tag("method", "PIX")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should record duration for payment processing")
    void shouldRecordPaymentDuration() {
        redMetricsService.recordPaymentDuration(Duration.ofMillis(85));

        var timer = meterRegistry.find("payments.processing.duration").timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(85.0);
    }

    @Test
    @DisplayName("Should handle missing meter registry with internal default fallback")
    void shouldHandleMissingMeterRegistryWithFallback() {
        RedMetricsService fallbackService = new RedMetricsService(Optional.empty());
        fallbackService.recordOrderCreated("CONFIRMED", "PICKUP");
        fallbackService.recordPaymentProcessed("PAID", "CASH");
        // Should not throw any exception
    }
}

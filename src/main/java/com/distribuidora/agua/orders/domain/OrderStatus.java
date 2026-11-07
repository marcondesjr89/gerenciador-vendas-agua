package com.distribuidora.agua.orders.domain;

/**
 * State machine representation for mineral water orders.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED;

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }
}

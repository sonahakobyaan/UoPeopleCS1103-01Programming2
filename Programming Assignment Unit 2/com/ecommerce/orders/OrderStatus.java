package com.ecommerce.orders;

/**
 * Represents the allowed states of an order, preventing invalid
 * or arbitrary status strings from being set.
 */
public enum OrderStatus {
    PENDING,
    SHIPPED,
    DELIVERED,
    CANCELLED
}

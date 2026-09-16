package com.ecommerce;

/**
 * Custom checked exception thrown when a customer tries to add
 * more of a product to the cart than is currently in stock.
 */
public class InsufficientStockException extends Exception {
    public InsufficientStockException(String message) {
        super(message);
    }
}

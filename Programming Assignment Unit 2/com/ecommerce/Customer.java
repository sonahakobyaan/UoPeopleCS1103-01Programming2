package com.ecommerce;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a customer who can browse products, manage a shopping
 * cart, and calculate the total cost of items before ordering.
 */
public class Customer {
    private int customerId;
    private String name;
    private Map<Product, Integer> shoppingCart;

    public Customer(int customerId, String name) {
        setCustomerId(customerId);
        setName(name);
        this.shoppingCart = new LinkedHashMap<>();
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        if (customerId <= 0) {
            throw new IllegalArgumentException("Customer ID must be positive.");
        }
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be empty.");
        }
        this.name = name;
    }

    /**
     * Returns a read-only view of the shopping cart so external code
     * cannot bypass validation by modifying the map directly.
     */
    public Map<Product, Integer> getShoppingCart() {
        return Collections.unmodifiableMap(shoppingCart);
    }

    /**
     * Adds a product to the shopping cart after validating quantity and
     * checking that the TOTAL quantity requested (existing + new) does
     * not exceed available stock.
     */
    public void addProductToCart(Product product, int quantity) throws InsufficientStockException {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        int alreadyInCart = shoppingCart.getOrDefault(product, 0);
        int totalRequested = alreadyInCart + quantity;

        if (totalRequested > product.getStockQuantity()) {
            throw new InsufficientStockException(
                    "Not enough stock for " + product.getName() +
                    ". Available: " + product.getStockQuantity() +
                    ", already in cart: " + alreadyInCart);
        }
        shoppingCart.put(product, totalRequested);
    }

    /**
     * Removes a quantity of a product from the cart, with validation
     * on the quantity argument.
     */
    public void removeProductFromCart(Product product, int quantity) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to remove must be greater than zero.");
        }
        if (!shoppingCart.containsKey(product)) {
            System.out.println("Product not found in cart: " + product.getName());
            return;
        }
        int currentQty = shoppingCart.get(product);
        if (quantity >= currentQty) {
            shoppingCart.remove(product);
        } else {
            shoppingCart.put(product, currentQty - quantity);
        }
    }

    public double calculateTotalCost() {
        double total = 0;
        for (Map.Entry<Product, Integer> entry : shoppingCart.entrySet()) {
            total += entry.getKey().getPrice() * entry.getValue();
        }
        return total;
    }

    /**
     * Finalizes checkout: returns a snapshot of the current cart contents
     * and empties the live cart. Intended to be called once, by Order,
     * at the moment an order is placed.
     */
    public Map<Product, Integer> checkout() {
        Map<Product, Integer> snapshot = new LinkedHashMap<>(shoppingCart);
        shoppingCart.clear();
        return snapshot;
    }

    @Override
    public String toString() {
        return String.format("Customer[ID=%d, Name=%s]", customerId, name);
    }
}

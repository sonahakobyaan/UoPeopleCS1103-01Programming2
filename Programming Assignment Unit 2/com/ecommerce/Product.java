package com.ecommerce;

/**
 * Represents a product available for purchase in the online store.
 * Includes validation to ensure product data stays consistent.
 */
public class Product {
    private int productId;
    private String name;
    private double price;
    private int stockQuantity;

    public Product(int productId, String name, double price, int stockQuantity) {
        setProductId(productId);
        setName(name);
        setPrice(price);
        setStockQuantity(stockQuantity);
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        if (productId <= 0) {
            throw new IllegalArgumentException("Product ID must be a positive number.");
        }
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty.");
        }
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        if (Double.isNaN(price) || Double.isInfinite(price) || price < 0) {
            throw new IllegalArgumentException("Price must be a valid non-negative number.");
        }
        this.price = price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative.");
        }
        this.stockQuantity = stockQuantity;
    }

    /**
     * Reduces available stock when an order is placed.
     * Validates that the quantity is positive and does not exceed
     * what is currently available.
     * @param quantity amount to subtract from stock
     */
    public void reduceStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to reduce must be greater than zero.");
        }
        if (quantity > stockQuantity) {
            throw new IllegalArgumentException("Not enough stock available for " + name);
        }
        stockQuantity -= quantity;
    }

    @Override
    public String toString() {
        return String.format("Product[ID=%d, Name=%s, Price=$%.2f, Stock=%d]",
                productId, name, price, stockQuantity);
    }
}

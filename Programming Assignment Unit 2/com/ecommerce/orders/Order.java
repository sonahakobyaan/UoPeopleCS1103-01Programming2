package com.ecommerce.orders;

import com.ecommerce.Customer;
import com.ecommerce.Product;

import java.util.Map;

/**
 * Represents an order placed by a customer. Handles order totals,
 * status updates, and generating a readable order summary.
 */
public class Order {
    private static int orderCounter = 1000;

    private int orderId;
    private Customer customer;
    private Map<Product, Integer> orderedProducts;
    private double orderTotal;
    private OrderStatus orderStatus;

    public Order(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null.");
        }

        Map<Product, Integer> cartContents = customer.getShoppingCart();
        if (cartContents.isEmpty()) {
            throw new IllegalStateException("Cannot place an order with an empty shopping cart.");
        }

        // Validate that every item in the cart can still be fulfilled
        // BEFORE touching the customer's cart or any product's stock.
        for (Map.Entry<Product, Integer> entry : cartContents.entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();
            if (quantity > product.getStockQuantity()) {
                throw new IllegalStateException(
                        "Cannot place order: insufficient stock for " + product.getName() +
                        ". Requested: " + quantity + ", Available: " + product.getStockQuantity());
            }
        }

        // All items validated -- now it is safe to finalize checkout.
        this.orderId = ++orderCounter;
        this.customer = customer;
        this.orderedProducts = customer.checkout();

        this.orderTotal = 0;
        for (Map.Entry<Product, Integer> entry : orderedProducts.entrySet()) {
            entry.getKey().reduceStock(entry.getValue());
            orderTotal += entry.getKey().getPrice() * entry.getValue();
        }

        this.orderStatus = OrderStatus.PENDING;
    }

    public int getOrderId() {
        return orderId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public double getOrderTotal() {
        return orderTotal;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    /**
     * Updates the order status to one of the predefined OrderStatus values,
     * preventing arbitrary or invalid status strings.
     */
    public void updateOrderStatus(OrderStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Order status cannot be null.");
        }
        this.orderStatus = newStatus;
    }

    public String generateOrderSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("----- Order Summary -----\n");
        sb.append("Order ID: ").append(orderId).append("\n");
        sb.append("Customer: ").append(customer.getName())
                .append(" (ID: ").append(customer.getCustomerId()).append(")\n");
        sb.append("Status: ").append(orderStatus).append("\n");
        sb.append("Items:\n");
        for (Map.Entry<Product, Integer> entry : orderedProducts.entrySet()) {
            Product p = entry.getKey();
            int qty = entry.getValue();
            sb.append(String.format("  - %s x%d = $%.2f%n", p.getName(), qty, p.getPrice() * qty));
        }
        sb.append(String.format("Order Total: $%.2f%n", orderTotal));
        sb.append("--------------------------");
        return sb.toString();
    }
}

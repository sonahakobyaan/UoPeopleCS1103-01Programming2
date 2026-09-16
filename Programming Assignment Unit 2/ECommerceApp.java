import com.ecommerce.Customer;
import com.ecommerce.InsufficientStockException;
import com.ecommerce.Product;
import com.ecommerce.orders.Order;
import com.ecommerce.orders.OrderStatus;

/**
 * Main program demonstrating the e-commerce system: browsing products,
 * managing a shopping cart, and placing an order, including error handling.
 */
public class ECommerceApp {
    public static void main(String[] args) {
        System.out.println("=== Welcome to the Simple E-Commerce System ===\n");

        // Create products
        Product laptop = new Product(101, "Laptop", 899.99, 10);
        Product headphones = new Product(102, "Wireless Headphones", 59.99, 25);
        Product keyboard = new Product(103, "Mechanical Keyboard", 45.50, 15);

        System.out.println("Available Products:");
        System.out.println(laptop);
        System.out.println(headphones);
        System.out.println(keyboard);
        System.out.println();

        // Demonstrate input validation catching bad product data
        try {
            Product invalidProduct1 = new Product(104, "", 10.0, 5);
        } catch (IllegalArgumentException e) {
            System.out.println("Validation Error (name): " + e.getMessage());
        }

        try {
            Product invalidProduct2 = new Product(105, "Invalid Product", -20.0, 5);
        } catch (IllegalArgumentException e) {
            System.out.println("Validation Error (price): " + e.getMessage());
        }

        try {
            Product invalidProduct3 = new Product(106, "Invalid Product", 10.0, -5);
        } catch (IllegalArgumentException e) {
            System.out.println("Validation Error (stock): " + e.getMessage());
        }
        System.out.println();

        // Create a customer
        Customer customer = new Customer(1, "John Smith");
        System.out.println("Customer created: " + customer);
        System.out.println();

        // Browse and add products to the cart
        try {
            customer.addProductToCart(laptop, 1);
            customer.addProductToCart(headphones, 2);
            customer.addProductToCart(keyboard, 1);
            System.out.println(customer.getName() + " added items to the shopping cart.");
        } catch (InsufficientStockException e) {
            System.out.println("Error adding to cart: " + e.getMessage());
        }

        // Demonstrate exception handling for insufficient stock
        try {
            customer.addProductToCart(laptop, 100);
        } catch (InsufficientStockException e) {
            System.out.println("Expected error: " + e.getMessage());
        }
        System.out.println();

        // Demonstrate quantity validation on removal
        try {
            customer.removeProductFromCart(keyboard, -1);
        } catch (IllegalArgumentException e) {
            System.out.println("Expected error removing from cart: " + e.getMessage());
        }
        System.out.println();

        System.out.printf("Current cart total: $%.2f%n%n", customer.calculateTotalCost());

        // Place the order
        try {
            Order order = new Order(customer);
            System.out.println(order.generateOrderSummary());
            System.out.println();

            order.updateOrderStatus(OrderStatus.SHIPPED);
            System.out.println("Order status updated.\n");
            System.out.println(order.generateOrderSummary());
        } catch (IllegalStateException e) {
            System.out.println("Order Error: " + e.getMessage());
        }
        System.out.println();

        // Demonstrate ordering with an empty cart
        Customer emptyCustomer = new Customer(2, "Jane Doe");
        try {
            Order badOrder = new Order(emptyCustomer);
        } catch (IllegalStateException e) {
            System.out.println("Expected error placing order: " + e.getMessage());
        }

        System.out.println("\nRemaining stock:");
        System.out.println(laptop);
        System.out.println(headphones);
        System.out.println(keyboard);
    }
}

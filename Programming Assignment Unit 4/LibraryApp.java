import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Command-line interface for the generic library catalog.
 * A mixed catalog (Books, DVDs and Magazines) is used.
 */
public class LibraryApp {

    private static final Scanner INPUT = new Scanner(System.in);
    private static final Catalog<String, LibraryItem<String>> CATALOG = new Catalog<>();

    public static void main(String[] args) {
        System.out.println("=== Generic Library Catalog ===");
        boolean running = true;

        while (running) {
            printMenu();
            String choice = prompt("Enter your choice: ").trim();

            switch (choice) {
                case "1":
                    addBook();
                    break;
                case "2":
                    addDvd();
                    break;
                case "3":
                    addMagazine();
                    break;
                case "4":
                    removeItem();
                    break;
                case "5":
                    viewItemDetails();
                    break;
                case "6":
                    viewCatalog();
                    break;
                case "7":
                    searchByAuthor();
                    break;
                case "0":
                    running = false;
                    System.out.println("Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number from the menu.");
            }
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("===== LIBRARY CATALOG MENU =====");
        System.out.println("1. Add a Book");
        System.out.println("2. Add a DVD");
        System.out.println("3. Add a Magazine");
        System.out.println("4. Remove an item");
        System.out.println("5. View item details");
        System.out.println("6. View entire catalog");
        System.out.println("7. Search by author");
        System.out.println("0. Exit");
    }

    // ---------- menu actions ----------

    private static void addBook() {
        try {
            String id = prompt("Item ID: ");
            String title = prompt("Title: ");
            String author = prompt("Author: ");
            int pages = promptInt("Number of pages: ");
            CATALOG.addItem(new Book(id.trim(), title, author, pages));
            System.out.println("Book added successfully.");
        } catch (DuplicateItemException | IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void addDvd() {
        try {
            String id = prompt("Item ID: ");
            String title = prompt("Title: ");
            String author = prompt("Director: ");
            int minutes = promptInt("Duration (minutes): ");
            CATALOG.addItem(new DVD(id.trim(), title, author, minutes));
            System.out.println("DVD added successfully.");
        } catch (DuplicateItemException | IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void addMagazine() {
        try {
            String id = prompt("Item ID: ");
            String title = prompt("Title: ");
            String author = prompt("Publisher: ");
            int issue = promptInt("Issue number: ");
            CATALOG.addItem(new Magazine(id.trim(), title, author, issue));
            System.out.println("Magazine added successfully.");
        } catch (DuplicateItemException | IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void removeItem() {
        String id = prompt("Enter the ID of the item to remove: ").trim();
        try {
            LibraryItem<String> removed = CATALOG.removeItem(id);
            System.out.println("Removed: " + removed.getTitle() + " (" + removed.getItemID() + ")");
        } catch (ItemNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void viewItemDetails() {
        String id = prompt("Enter the ID of the item: ").trim();
        try {
            System.out.println(CATALOG.getItem(id).getDetails());
        } catch (ItemNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void viewCatalog() {
        if (CATALOG.isEmpty()) {
            System.out.println("The catalog is currently empty.");
            return;
        }
        System.out.println("--- Current Catalog (" + CATALOG.size() + " item(s)) ---");
        for (LibraryItem<String> item : CATALOG.getAllItems()) {
            System.out.println(item.getDetails());
        }
    }

    private static void searchByAuthor() {
        try {
            List<LibraryItem<String>> found = CATALOG.findByAuthor(prompt("Author to search for: "));
            if (found.isEmpty()) {
                System.out.println("No items found for that author.");
            } else {
                for (LibraryItem<String> item : found) {
                    System.out.println(item.getDetails());
                }
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // ---------- input helpers ----------

    /** Shows a message and reads one line; returns "0" (exit) if input has ended. */
    private static String prompt(String message) {
        System.out.print(message);
        try {
            return INPUT.nextLine();
        } catch (NoSuchElementException e) {
            return "0";
        }
    }

    /** Reads a whole number, converting bad input into a clear error message. */
    private static int promptInt(String message) {
        String text = prompt(message).trim();
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + text + "' is not a valid whole number.");
        }
    }
}

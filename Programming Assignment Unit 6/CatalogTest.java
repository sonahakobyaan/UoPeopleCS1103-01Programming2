import java.util.Arrays;
import java.util.List;

/**
 * Tests for Catalog and LibraryItem. Run this class's main method;
 * each test prints PASS or FAIL and a summary is shown at the end.
 */
public class CatalogTest {

    private static int passed = 0;
    private static int failed = 0;

    /** One test: returns true if the behaviour was correct. */
    private interface TestCase {
        boolean run() throws Exception;
    }

    /** A custom item type with an Integer ID, to prove the catalog is truly generic. */
    private static class Audiobook extends LibraryItem<Integer> {
        Audiobook(int id, String title, String author) {
            super(id, title, author);
        }

        @Override
        public String getItemType() {
            return "Audiobook";
        }
    }

    private static void run(String name, TestCase test) {
        try {
            report(name, test.run());
        } catch (Exception e) {
            report(name + " (unexpected " + e.getClass().getSimpleName() + ")", false);
        }
    }

    private static void report(String name, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("[PASS] " + name);
        } else {
            failed++;
            System.out.println("[FAIL] " + name);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Running Catalog Tests ===");

        run("Test 1: Add a Book to a Book-only catalog", () -> {
            Catalog<String, Book> c = new Catalog<>();
            c.addItem(new Book("B001", "Clean Code", "Robert C. Martin", 464));
            return c.size() == 1 && c.contains("B001");
        });

        run("Test 2: Mixed catalog holds Book, DVD and Magazine", () -> {
            Catalog<String, LibraryItem<String>> c = new Catalog<>();
            c.addItem(new Book("B001", "Clean Code", "Robert C. Martin", 464));
            c.addItem(new DVD("D001", "Inception", "Christopher Nolan", 148));
            c.addItem(new Magazine("M001", "National Geographic", "NatGeo Society", 202));
            return c.size() == 3;
        });

        run("Test 3: Retrieve item details by ID", () -> {
            Catalog<String, LibraryItem<String>> c = new Catalog<>();
            c.addItem(new DVD("D001", "Inception", "Christopher Nolan", 148));
            LibraryItem<String> item = c.getItem("D001");
            return item.getTitle().equals("Inception") && item.getDetails().contains("148");
        });

        run("Test 4: Remove an existing item", () -> {
            Catalog<String, Book> c = new Catalog<>();
            c.addItem(new Book("B001", "Clean Code", "Robert C. Martin", 464));
            Book removed = c.removeItem("B001");
            return removed.getTitle().equals("Clean Code") && !c.contains("B001") && c.size() == 0;
        });

        run("Test 5: Removing a non-existent item throws ItemNotFoundException", () -> {
            Catalog<String, Book> c = new Catalog<>();
            try {
                c.removeItem("X999");
                return false;
            } catch (ItemNotFoundException e) {
                return true;
            }
        });

        run("Test 6: Adding a duplicate ID throws DuplicateItemException", () -> {
            Catalog<String, Book> c = new Catalog<>();
            c.addItem(new Book("B001", "Clean Code", "Robert C. Martin", 464));
            try {
                c.addItem(new Book("B001", "Another Title", "Someone Else", 100));
                return false;
            } catch (DuplicateItemException e) {
                return c.size() == 1;
            }
        });

        run("Test 7: Adding null throws IllegalArgumentException", () -> {
            Catalog<String, Book> c = new Catalog<>();
            try {
                c.addItem(null);
                return false;
            } catch (IllegalArgumentException e) {
                return true;
            }
        });

        run("Test 8: Blank title is rejected", () -> {
            try {
                new Book("B002", "   ", "Some Author", 10);
                return false;
            } catch (IllegalArgumentException e) {
                return true;
            }
        });

        run("Test 9: Non-positive page count is rejected", () -> {
            try {
                new Book("B003", "Title", "Author", 0);
                return false;
            } catch (IllegalArgumentException e) {
                return true;
            }
        });

        run("Test 10: Catalog works with Integer IDs (Audiobook)", () -> {
            Catalog<Integer, Audiobook> c = new Catalog<>();
            c.addItem(new Audiobook(101, "Dune", "Frank Herbert"));
            c.addItem(new Audiobook(102, "Emma", "Jane Austen"));
            return c.size() == 2 && c.getItem(102).getTitle().equals("Emma");
        });

        run("Test 11: Search by author is case-insensitive", () -> {
            Catalog<String, Book> c = new Catalog<>();
            c.addItem(new Book("B001", "Clean Code", "Robert C. Martin", 464));
            c.addItem(new Book("B002", "Clean Architecture", "Robert C. Martin", 432));
            c.addItem(new Book("B003", "Effective Java", "Joshua Bloch", 416));
            return c.findByAuthor("martin").size() == 2 && c.findByAuthor("BLOCH").size() == 1;
        });

        run("Test 12: getItemsOfType filters by subtype", () -> {
            Catalog<String, LibraryItem<String>> c = new Catalog<>();
            c.addItem(new Book("B001", "Clean Code", "Robert C. Martin", 464));
            c.addItem(new Book("B002", "Effective Java", "Joshua Bloch", 416));
            c.addItem(new DVD("D001", "Inception", "Christopher Nolan", 148));
            return c.getItemsOfType(Book.class).size() == 2
                    && c.getItemsOfType(DVD.class).size() == 1
                    && c.getItemsOfType(Magazine.class).isEmpty();
        });

        run("Test 13: addAll accepts a List<Book> via wildcard", () -> {
            Catalog<String, LibraryItem<String>> c = new Catalog<>();
            List<Book> books = Arrays.asList(
                    new Book("B001", "Clean Code", "Robert C. Martin", 464),
                    new Book("B002", "Effective Java", "Joshua Bloch", 416));
            c.addAll(books);
            return c.size() == 2;
        });

        run("Test 14: getAllItems returns items sorted by ID", () -> {
            Catalog<String, Book> c = new Catalog<>();
            c.addItem(new Book("B003", "Third", "Author C", 30));
            c.addItem(new Book("B001", "First", "Author A", 10));
            c.addItem(new Book("B002", "Second", "Author B", 20));
            List<Book> sorted = c.getAllItems();
            return sorted.get(0).getItemID().equals("B001")
                    && sorted.get(2).getItemID().equals("B003");
        });

        run("Test 15: A new catalog is empty", () -> {
            Catalog<String, Book> c = new Catalog<>();
            return c.isEmpty() && c.getAllItems().isEmpty() && c.size() == 0;
        });

        run("Test 16: addAll is atomic when a duplicate exists", () -> {
            Catalog<String, Book> c = new Catalog<>();
            c.addItem(new Book("B001", "Existing", "Author A", 100));
            List<Book> books = Arrays.asList(
                    new Book("B002", "New Book", "Author B", 200),
                    new Book("B001", "Duplicate", "Author C", 300));
            try {
                c.addAll(books);
                return false;
            } catch (DuplicateItemException e) {
                return c.size() == 1 && !c.contains("B002");
            }
        });

        run("Test 17: addAll rejects a null collection", () -> {
            Catalog<String, Book> c = new Catalog<>();
            try {
                c.addAll(null);
                return false;
            } catch (IllegalArgumentException e) {
                return true;
            }
        });

        run("Test 18: addAll rejects a collection containing null and adds nothing", () -> {
            Catalog<String, Book> c = new Catalog<>();
            List<Book> books = Arrays.asList(
                    new Book("B001", "First", "Author A", 100),
                    null);
            try {
                c.addAll(books);
                return false;
            } catch (IllegalArgumentException e) {
                return c.isEmpty();
            }
        });

        System.out.println("=== Results: " + passed + " passed, " + failed + " failed ===");
    }
}

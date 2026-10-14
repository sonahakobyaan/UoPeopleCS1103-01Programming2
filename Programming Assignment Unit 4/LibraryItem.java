/**
 * Abstract generic base class for every item a library can hold.
 *
 * @param <ID> the type of the unique identifier (e.g. String or Integer).
 *             It must be Comparable so catalogs can sort items by ID.
 */
public abstract class LibraryItem<ID extends Comparable<ID>> {

    private final ID itemID;
    private final String title;
    private final String author;

    /**
     * Creates a library item.
     *
     * @param itemID unique identifier (must not be null)
     * @param title  title of the item (must not be blank)
     * @param author author/creator of the item (must not be blank)
     * @throws IllegalArgumentException if any argument is null or blank
     */
    protected LibraryItem(ID itemID, String title, String author) {
        if (itemID == null) {
            throw new IllegalArgumentException("Item ID must not be null.");
        }
        if (isBlank(title)) {
            throw new IllegalArgumentException("Title must not be empty.");
        }
        if (isBlank(author)) {
            throw new IllegalArgumentException("Author must not be empty.");
        }
        this.itemID = itemID;
        this.title = title.trim();
        this.author = author.trim();
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    public ID getItemID() {
        return itemID;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    /** @return a short label for the kind of item, e.g. "Book" or "DVD". */
    public abstract String getItemType();

    /** @return a one-line, human-readable description of this item. */
    public String getDetails() {
        return String.format("%-9s | ID: %-6s | Title: %s | Author: %s",
                getItemType(), itemID, title, author);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        LibraryItem<?> that = (LibraryItem<?>) other;
        return itemID.equals(that.itemID);
    }

    @Override
    public int hashCode() {
        return itemID.hashCode();
    }

    @Override
    public String toString() {
        return getDetails();
    }
}

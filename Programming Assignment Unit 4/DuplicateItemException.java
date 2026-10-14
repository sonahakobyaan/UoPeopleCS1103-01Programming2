/** Thrown when an item with the same ID already exists in the catalog. */
public class DuplicateItemException extends Exception {

    public DuplicateItemException(String message) {
        super(message);
    }
}

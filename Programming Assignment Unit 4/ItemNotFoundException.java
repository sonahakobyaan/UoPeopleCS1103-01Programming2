/** Thrown when an operation refers to an item that is not in the catalog. */
public class ItemNotFoundException extends Exception {

    public ItemNotFoundException(String message) {
        super(message);
    }
}

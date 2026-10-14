import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A generic catalog that stores library items by their ID.
 *
 * @param <ID> the type of the item identifier
 * @param <T>  the type of item stored (Book, DVD, Magazine, or LibraryItem
 *             itself for a mixed catalog)
 */
public class Catalog<ID extends Comparable<ID>, T extends LibraryItem<ID>> {

    /** Maps each item ID to its item; keeps insertion order. */
    private final Map<ID, T> items = new LinkedHashMap<>();

    /**
     * Adds a new item to the catalog.
     *
     * @param item the item to add
     * @throws DuplicateItemException   if an item with the same ID already exists
     * @throws IllegalArgumentException if the item is null
     */
    public void addItem(T item) throws DuplicateItemException {
        if (item == null) {
            throw new IllegalArgumentException("Cannot add a null item to the catalog.");
        }
        if (items.containsKey(item.getItemID())) {
            throw new DuplicateItemException(
                    "An item with ID '" + item.getItemID() + "' already exists in the catalog.");
        }
        items.put(item.getItemID(), item);
    }

    /**
     * Generic method using a wildcard: adds every item from any collection
     * whose elements are T or a subtype of T. The operation is atomic:
     * if any item is invalid or a duplicate, nothing is added.
     *
     * @param newItems the items to add
     * @throws DuplicateItemException   if any ID already exists (in the catalog
     *                                  or within newItems itself)
     * @throws IllegalArgumentException if the collection or any item is null
     */
    public void addAll(Collection<? extends T> newItems) throws DuplicateItemException {
        if (newItems == null) {
            throw new IllegalArgumentException("The collection must not be null.");
        }

        Map<ID, T> pendingItems = new LinkedHashMap<>();

        // Validate every item before modifying the catalog.
        for (T item : newItems) {
            if (item == null) {
                throw new IllegalArgumentException("Cannot add a null item to the catalog.");
            }
            ID id = item.getItemID();
            if (items.containsKey(id) || pendingItems.containsKey(id)) {
                throw new DuplicateItemException(
                        "An item with ID '" + id + "' already exists in the catalog.");
            }
            pendingItems.put(id, item);
        }

        // Commit only after every item has passed validation.
        items.putAll(pendingItems);
    }

    /**
     * Removes an item from the catalog.
     *
     * @param itemID the ID of the item to remove
     * @return the item that was removed
     * @throws ItemNotFoundException if no item has that ID
     */
    public T removeItem(ID itemID) throws ItemNotFoundException {
        T removed = items.remove(itemID);
        if (removed == null) {
            throw new ItemNotFoundException(
                    "Cannot remove: no item with ID '" + itemID + "' exists in the catalog.");
        }
        return removed;
    }

    /**
     * Retrieves an item by ID.
     *
     * @param itemID the ID to look up
     * @return the matching item
     * @throws ItemNotFoundException if no item has that ID
     */
    public T getItem(ID itemID) throws ItemNotFoundException {
        T item = items.get(itemID);
        if (item == null) {
            throw new ItemNotFoundException(
                    "No item with ID '" + itemID + "' exists in the catalog.");
        }
        return item;
    }

    /** @return true if an item with this ID is in the catalog. */
    public boolean contains(ID itemID) {
        return items.containsKey(itemID);
    }

    /** @return the number of items in the catalog. */
    public int size() {
        return items.size();
    }

    /** @return true if the catalog holds no items. */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /** @return all items, sorted by ID (a new list; changing it does not affect the catalog). */
    public List<T> getAllItems() {
        List<T> sorted = new ArrayList<>(items.values());
        sorted.sort((a, b) -> a.getItemID().compareTo(b.getItemID()));
        return sorted;
    }

    /**
     * Finds items whose author contains the given text (case-insensitive).
     *
     * @param authorText part of an author name
     * @return matching items, sorted by ID (empty list if none)
     * @throws IllegalArgumentException if the search text is blank
     */
    public List<T> findByAuthor(String authorText) {
        if (authorText == null || authorText.trim().isEmpty()) {
            throw new IllegalArgumentException("Search text must not be empty.");
        }
        String needle = authorText.trim().toLowerCase();
        List<T> matches = new ArrayList<>();
        for (T item : getAllItems()) {
            if (item.getAuthor().toLowerCase().contains(needle)) {
                matches.add(item);
            }
        }
        return matches;
    }

    /**
     * Generic method returning only the items of one specific subtype,
     * e.g. getItemsOfType(DVD.class).
     *
     * @param type the subtype to filter for
     * @param <R>  the subtype (must be T or a subclass of T)
     * @return items of that subtype, sorted by ID
     */
    public <R extends T> List<R> getItemsOfType(Class<R> type) {
        List<R> result = new ArrayList<>();
        for (T item : getAllItems()) {
            if (type.isInstance(item)) {
                result.add(type.cast(item));
            }
        }
        return result;
    }
}

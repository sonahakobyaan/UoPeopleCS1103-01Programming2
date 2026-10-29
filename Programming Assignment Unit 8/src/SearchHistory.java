import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Keeps the most recent successful searches, newest first, each stamped with
 * the date and time it was made.
 */
public class SearchHistory {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** One history row. */
    public record Entry(String query, String label, LocalDateTime when) {

        /** Text shown in the history list, e.g. "2026-10-28 14:05:33  London, GB". */
        public String display() {
            return when.format(STAMP) + "  " + label;
        }
    }

    private final int capacity;
    private final List<Entry> entries = new ArrayList<>();

    public SearchHistory(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    /** Records a search; the oldest entry is dropped when the list is full. */
    public void add(String query, String label, LocalDateTime when) {
        entries.add(0, new Entry(query, label, when));
        while (entries.size() > capacity) {
            entries.remove(entries.size() - 1);
        }
    }

    /** Read-only view, newest first. */
    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }
}

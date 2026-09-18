import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Clock.java
 *
 * Represents the shared "current time" resource in the application.
 *
 * DESIGN NOTE (connects to Eck, Section 12.1.3, "Mutual Exclusion with
 * synchronized"):
 * The formatted time string is written by the background ClockUpdateThread
 * once every second and read by the ClockDisplayThread roughly once every
 * second as well. Because two different threads access the same instance
 * variable, this is a shared resource, and Eck warns that "when two threads
 * need access to the same resource ... some care must be taken that they
 * don't try to use the same resource at the same time" (Section 12.1.3).
 * Even though a single String reference assignment is small, relying on
 * un-synchronized access to a non-volatile field gives no guarantee that a
 * value written by one thread will ever become visible to another thread
 * (Eck, Section 12.1.4, "Volatile Variables"). To follow the pattern Eck
 * demonstrates with the ThreadSafeCounter class, both the setter and the
 * getter here are declared synchronized on "this" object, so the update
 * thread and the display thread are guaranteed to see a consistent,
 * up-to-date value with no race condition.
 */
public class Clock {

    // The format requested by the assignment: HH:mm:ss dd-MM-yyyy
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy");

    // Shared resource written by the update thread, read by the display thread.
    private String currentTime;

    /**
     * Refreshes the stored time/date string with the current instant.
     * Synchronized so that no thread ever reads a half-written value.
     */
    public synchronized void updateTime() {
        currentTime = LocalDateTime.now().format(FORMATTER);
    }

    /**
     * Returns the most recently computed time/date string.
     * Synchronized on the same object as updateTime() so that the two
     * methods have mutually exclusive access to currentTime, exactly as
     * described for ThreadSafeCounter in Eck, Section 12.1.3.
     */
    public synchronized String getCurrentTime() {
        return currentTime;
    }
}

/**
 * ClockDisplayThread.java
 *
 * Foreground thread responsible for printing the Clock's current time to
 * the console once per second.
 *
 * DESIGN NOTE (connects to Eck, Section 12.1.1, "Creating and Running
 * Threads"):
 * Like ClockUpdateThread, this class extends Thread and overrides run(),
 * following Eck's first thread-programming technique. Keeping the update
 * logic and the display logic in two separate Thread subclasses mirrors
 * the "single responsibility" idea Eck raises when comparing extending
 * Thread versus implementing Runnable (Section 12.1.1): each class has one
 * clear job -- one keeps the time current, the other reports it.
 *
 * DESIGN NOTE (connects to Eck, Section 12.1.2, "Operations on Threads",
 * and Samoylov, Chapter 11, "Threads"):
 * This thread reads the shared value only through Clock's synchronized
 * getCurrentTime() method, never by touching a shared field directly.
 * Samoylov's discussion of thread creation (pp. 381-387) and Eck's warning
 * that "synchronization ... only guarantees mutual exclusion among all the
 * threads that are synchronized" (Section 12.1.3) both motivate always
 * going through the accessor rather than bypassing it.
 */
public class ClockDisplayThread extends Thread {

    private final Clock clock;
    private volatile boolean running = true;

    public ClockDisplayThread(Clock clock) {
        super("ClockDisplayThread");
        this.clock = clock;
    }

    @Override
    public void run() {
        while (running) {
            String time = clock.getCurrentTime();
            if (time != null) {
                System.out.println(time);
            }
            try {
                Thread.sleep(1000); // display once per second
            } catch (InterruptedException e) {
                running = false;
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Signals this thread to stop, using the same volatile-flag pattern
     * described in Eck, Section 12.1.4.
     */
    public void terminate() {
        running = false;
        this.interrupt();
    }
}

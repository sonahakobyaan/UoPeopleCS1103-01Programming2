/**
 * SimpleClockApplication.java
 *
 * Entry point for the Simple Clock Application assignment.
 *
 * PURPOSE
 * Demonstrates the basic Java Thread model by running two cooperating
 * threads:
 *   1. ClockUpdateThread  -- a background thread that keeps the current
 *      time/date up to date once per second.
 *   2. ClockDisplayThread -- a foreground thread that prints the current
 *      time/date to the console once per second.
 *
 * DESIGN NOTE (connects to Eck, Section 12.1.2, "Operations on Threads",
 * subsection on thread priority):
 * Eck explains that "a thread with a greater priority value will be run in
 * preference to a thread with a smaller priority" and that this is useful,
 * for example, "for computations that can be done in the background, when
 * no more important thread has work to do" (Section 12.1.2). The assignment
 * calls for the clock DISPLAY thread to have a higher priority than the
 * background UPDATE thread, since responsiveness of the visible output
 * matters more than the exact instant the internal value is refreshed.
 * Following Eck's recommended pattern of setting a priority relative to
 * the currently running thread --
 *     thrd.setPriority( Thread.NORM_PRIORITY - 1 );
 * (Section 12.1.2) -- the update thread is given a priority one below
 * normal, and the display thread one above normal.
 *
 * DESIGN NOTE (connects to Eck, Section 12.1.2, "join()"):
 * When the application decides to stop, it signals both threads to
 * terminate (Section 12.1.4's volatile-flag technique) and then calls
 * join() on each one. Eck explains that "if another thread calls
 * thrd.join(), that other thread will go to sleep until thrd terminates"
 * (Section 12.1.2) -- this guarantees the main thread does not print its
 * final "terminated" message until both worker threads have actually
 * exited.
 *
 * DESIGN NOTE (connects to Samoylov, Chapter 11, "Application
 * termination"):
 * Samoylov notes that "without forced termination, the JVM instance
 * continues running until the main application thread and all child user
 * threads are completed" (pp. 381-387). Both worker threads here are
 * ordinary user threads (not daemon threads), which is why the program
 * explicitly signals them to stop and joins them, rather than simply
 * letting main() return and relying on the JVM to exit around them.
 */
public class SimpleClockApplication {

    // How long the demonstration runs before shutting itself down.
    // (In an interactive application this would instead run until the
    // user closed a window or typed a "quit" command.)
    private static final long DEMO_DURATION_MILLIS = 15_000;

    public static void main(String[] args) {
        Clock clock = new Clock();

        // Compute an initial value synchronously so the display thread
        // never has to handle a null time on its very first read.
        clock.updateTime();

        ClockUpdateThread updateThread = new ClockUpdateThread(clock);
        ClockDisplayThread displayThread = new ClockDisplayThread(clock);

        try {
            // --- Thread Priorities -------------------------------------
            // Display thread: higher priority (more important to the user).
            // Update thread:  lower priority (pure background bookkeeping).
            updateThread.setPriority(Thread.NORM_PRIORITY - 1);
            displayThread.setPriority(Thread.NORM_PRIORITY + 1);
        } catch (IllegalArgumentException | SecurityException e) {
            // Eck (Section 12.1.2) notes setPriority() can throw
            // IllegalArgumentException or SecurityException; the program
            // should still work correctly even if priorities can't be set.
            System.out.println("Warning: could not set thread priorities: " + e);
        }

        System.out.println("Starting Simple Clock Application...");
        System.out.println("(Display thread priority = " + displayThread.getPriority()
                + ", Update thread priority = " + updateThread.getPriority() + ")");
        System.out.println();

        updateThread.start();
        displayThread.start();

        // Let the clock run for a fixed demonstration period.
        try {
            Thread.sleep(DEMO_DURATION_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Signal both worker threads to stop, then wait for them to finish.
        updateThread.terminate();
        displayThread.terminate();

        try {
            updateThread.join();
            displayThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println();
        System.out.println("Clock application terminated.");
    }
}

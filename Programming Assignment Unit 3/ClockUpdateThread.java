/**
 * ClockUpdateThread.java
 *
 * Background thread responsible for continuously refreshing the Clock's
 * internal time value.
 *
 * DESIGN NOTE (connects to Eck, Section 12.1.1, "Creating and Running
 * Threads"):
 * This class follows the first of the two thread-programming techniques
 * Eck describes: "create a subclass of Thread and ... define the method
 * public void run() in the subclass" (Section 12.1.1). The run() method
 * here defines the task carried out by the thread -- repeatedly calling
 * clock.updateTime() and then sleeping -- exactly the pattern Eck uses in
 * the NamedThread example.
 *
 * DESIGN NOTE (connects to Eck, Section 12.1.4, "Volatile Variables"):
 * The boolean flag "running" is declared volatile because it is set by one
 * thread (the main thread, when the application shuts down) and read by a
 * different thread (this background thread) outside of any synchronized
 * block. Eck explains that "if a variable is declared to be volatile, no
 * thread will keep a local copy of that variable in its cache ... This
 * makes it safe for threads to refer to volatile shared variables even
 * outside of synchronized code" and gives the exact same
 * "signal a thread to terminate" use case as the canonical example
 * (Section 12.1.4).
 *
 * DESIGN NOTE (connects to Eck, Section 12.1.2, "Operations on Threads"):
 * Thread.sleep() is used to pace the updates once per second, and the
 * mandatory InterruptedException is caught, matching the pattern Eck shows
 * for Thread.sleep() usage.
 */
public class ClockUpdateThread extends Thread {

    private final Clock clock;
    private volatile boolean running = true;

    public ClockUpdateThread(Clock clock) {
        // Naming the thread makes it easy to identify in a debugger or in
        // thread-dump output -- good practice mentioned implicitly by Eck's
        // discussion of thread identity (Section 12.1.2).
        super("ClockUpdateThread");
        this.clock = clock;
    }

    @Override
    public void run() {
        while (running) {
            clock.updateTime();
            try {
                Thread.sleep(1000); // update once per second
            } catch (InterruptedException e) {
                // Treat an interrupt as a shutdown signal, as discussed in
                // Eck, Section 12.1.2, "Operations on Threads".
                running = false;
                Thread.currentThread().interrupt(); // preserve interrupt status
            }
        }
    }

    /**
     * Signals this thread to stop after its current sleep/update cycle.
     * Setting a volatile flag, rather than forcibly killing the thread, is
     * the "clean way for one thread to cause another thread to die"
     * recommended by Eck, Section 12.1.4.
     */
    public void terminate() {
        running = false;
        this.interrupt(); // wake the thread immediately if it is sleeping
    }
}

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * ChatClient connects to a ChatServer using a Socket and provides a simple
 * text-based user interface in the terminal.
 *
 * Two things must happen at the same time, so the client uses two threads:
 *  - The MAIN thread reads what the user types and sends it to the server.
 *  - The RECEIVER thread waits for messages from the server (which broadcasts
 *    messages from all users) and prints them on the screen.
 *
 * Usage: java ChatClient [host] [port]     (defaults: localhost 5000)
 */
public class ChatClient {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5000;
    private static final int MAX_PORT = 65535;
    private static final String QUIT_COMMAND = "/quit";
    private static final long SHUTDOWN_WAIT_MILLIS = 2000;

    private final String host;
    private final int port;

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /** Connects to the server, then sends typed messages until the user quits. */
    public void start() {
        try (Socket socket = new Socket(host, port);
             BufferedReader fromServer = new BufferedReader(
                     new InputStreamReader(
                             socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter toServer = new PrintWriter(
                     new OutputStreamWriter(
                             socket.getOutputStream(), StandardCharsets.UTF_8), true);
             Scanner keyboard = new Scanner(System.in)) {

            printBanner();

            // Receiver thread: shows every message the server broadcasts.
            Thread receiverThread = new Thread(() -> displayIncomingMessages(fromServer));
            receiverThread.setDaemon(true);
            receiverThread.start();

            sendTypedMessages(keyboard, toServer);

            // After /quit the server closes the connection; wait briefly so the
            // receiver can finish printing before the program ends.
            receiverThread.join(SHUTDOWN_WAIT_MILLIS);

        } catch (IOException e) {
            System.out.println("Could not connect to " + host + ":" + port
                    + " - " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Reads lines typed by the user and sends each one to the server. */
    private void sendTypedMessages(Scanner keyboard, PrintWriter toServer) {
        while (keyboard.hasNextLine()) {
            String message = keyboard.nextLine();
            toServer.println(message); // println + auto-flush sends it immediately
            if (message.trim().equalsIgnoreCase(QUIT_COMMAND)) {
                break;
            }
        }
    }

    /** Prints each line received from the server until the connection closes. */
    private void displayIncomingMessages(BufferedReader fromServer) {
        try {
            String message;
            while ((message = fromServer.readLine()) != null) {
                System.out.println(message);
            }
        } catch (IOException e) {
            // The socket was closed while waiting; this is the normal way to stop.
        }
        System.out.println("--- Disconnected from chat server ---");
        // Also stops the main thread if it is still waiting for keyboard input.
        System.exit(0);
    }

    private void printBanner() {
        System.out.println("==========================================");
        System.out.println("   ONLINE CHAT  -  connected to " + host + ":" + port);
        System.out.println("   Type a message and press Enter to send");
        System.out.println("   /users = list users    /quit = leave");
        System.out.println("==========================================");
    }

    public static void main(String[] args) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;
        try {
            if (args.length > 0) {
                host = args[0];
            }
            if (args.length > 1) {
                port = Integer.parseInt(args[1]);
                if (port < 1 || port > MAX_PORT) {
                    throw new NumberFormatException();
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Illegal port number: " + args[1]);
            return;
        }
        new ChatClient(host, port).start();
    }
}

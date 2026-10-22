import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ChatServer manages connections from multiple chat clients.
 *
 * How it works:
 *  1. A ServerSocket listens on a port (default 5000, above the reserved 1024).
 *  2. Each time accept() returns a new client Socket, the server assigns a unique
 *     user ID and wraps the connection in a ClientHandler.
 *  3. Every ClientHandler runs in its own thread (from a thread pool), so one
 *     slow or blocked client never stops the others from chatting.
 *  4. The server keeps a thread-safe map of all connected users (ID -> handler)
 *     and uses it to broadcast every message to every connected client.
 *
 * Usage: java ChatServer [port]
 */
public class ChatServer {

    private static final int DEFAULT_PORT = 5000;
    private static final int MAX_PORT = 65535;
    private static final String QUIT_COMMAND = "/quit";
    private static final String USERS_COMMAND = "/users";

    private final int port;

    // List of connected users. ConcurrentHashMap is safe to use from many threads.
    private final Map<Integer, ClientHandler> connectedUsers = new ConcurrentHashMap<>();

    // Produces the next unique user ID (1, 2, 3, ...) safely across threads.
    private final AtomicInteger nextUserId = new AtomicInteger(1);

    // Reuses threads for clients instead of creating an expensive new thread each time.
    private final ExecutorService clientThreadPool = Executors.newCachedThreadPool();

    public ChatServer(int port) {
        this.port = port;
    }

    /** Starts listening and accepts clients until the program is stopped (Ctrl+C). */
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log("Chat server started. Listening on port " + port);
            while (true) {
                // accept() blocks until a client connects.
                Socket clientSocket = serverSocket.accept();
                registerClient(clientSocket);
            }
        } catch (IOException e) {
            log("Server shut down with error: " + e.getMessage());
        } finally {
            clientThreadPool.shutdown();
        }
    }

    /** Assigns a unique ID to a new client, adds it to the user list, starts its thread. */
    private void registerClient(Socket clientSocket) {
        int userId = nextUserId.getAndIncrement();
        try {
            ClientHandler handler = new ClientHandler(clientSocket, userId);
            connectedUsers.put(userId, handler);
            clientThreadPool.execute(handler);
            log("User " + userId + " connected from " + clientSocket.getInetAddress()
                    + " (total users: " + connectedUsers.size() + ")");
        } catch (IOException e) {
            // A failure with one client must not crash the whole server.
            log("Could not set up User " + userId + ": " + e.getMessage());
            closeQuietly(clientSocket);
        }
    }

    /** Sends a message to every connected client. */
    private void broadcast(String message) {
        for (ClientHandler handler : connectedUsers.values()) {
            handler.sendMessage(message);
        }
    }

    /** Builds a sorted, readable list of the users currently connected. */
    private String buildUserList() {
        StringBuilder userList = new StringBuilder();
        userList.append("Connected users (").append(connectedUsers.size()).append("):");
        for (int userId : new TreeSet<>(connectedUsers.keySet())) {
            userList.append(" User ").append(userId);
        }
        return userList.toString();
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            // Nothing useful can be done if closing fails.
        }
    }

    private static void log(String message) {
        System.out.println("[SERVER] " + message);
    }

    /**
     * Handles all communication with ONE client. Runs in its own thread:
     * it reads lines from the client and broadcasts them to everyone.
     */
    private class ClientHandler implements Runnable {

        private final Socket socket;
        private final int userId;
        private final BufferedReader fromClient;
        private final PrintWriter toClient;

        ClientHandler(Socket socket, int userId) throws IOException {
            this.socket = socket;
            this.userId = userId;
            this.fromClient = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            // UTF-8 is set explicitly so every computer reads/writes text the same way.
            // 'true' = auto-flush after every println, so data is actually sent
            // right away (Eck, Section 11.4.4 explains why flushing matters).
            OutputStreamWriter utf8Output =
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
            this.toClient = new PrintWriter(utf8Output, true);
        }

        @Override
        public void run() {
            try {
                sendMessage("WELCOME! You are connected as User " + userId + ". Type "
                        + USERS_COMMAND + " to list users or " + QUIT_COMMAND + " to leave.");
                broadcast("*** User " + userId + " joined the chat ***");

                String line;
                // readLine() returns null when the client has disconnected.
                while ((line = fromClient.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) {
                        continue;
                    }
                    if (line.equalsIgnoreCase(QUIT_COMMAND)) {
                        break;
                    }
                    if (line.equalsIgnoreCase(USERS_COMMAND)) {
                        sendMessage(buildUserList());
                    } else {
                        broadcast("User " + userId + ": " + line);
                    }
                }
            } catch (IOException e) {
                log("Connection lost with User " + userId + ": " + e.getMessage());
            } finally {
                disconnect();
            }
        }

        /** Sends one line of text to this client. */
        void sendMessage(String message) {
            toClient.println(message);
        }

        /** Removes the user, closes the connection and tells everyone else. */
        private void disconnect() {
            connectedUsers.remove(userId);
            closeQuietly(socket);
            broadcast("*** User " + userId + " left the chat ***");
            log("User " + userId + " disconnected (total users: "
                    + connectedUsers.size() + ")");
        }
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
                if (port < 1 || port > MAX_PORT) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                System.out.println("Illegal port number: " + args[0]);
                return;
            }
        }
        new ChatServer(port).start();
    }
}

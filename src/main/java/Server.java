import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A server is responsible for accepting client connections and delegating client handling to a thread from the thread pool
 */
public class Server {
    /**
     * The port that the server listens on
     */
    private final int PORT;
    /**
     * The thread pool that the server uses for handling clients
     */
    private final ExecutorService executorService;
    /**
     * The philosopher that the server belongs to
     */
    private final Philosopher philosopher;
    /**
     * The logger for the server
     */
    private static final Logger logger = LogManager.getLogger(Server.class);
    /**
     * A latch that is used to wait for the server to finish
     */
    private final CountDownLatch serverLatch;

    /**
     * Create a new server
     *
     * @param philosopher The philosopher that the server belongs to
     * @param port        The port that the server listens on
     */
    public Server(Philosopher philosopher, int port) {
        this.executorService = Executors.newFixedThreadPool(2);
        this.philosopher = philosopher;
        this.PORT = port;
        this.serverLatch = new CountDownLatch(1); // Initialize the latch
    }

    /**
     * Start the server
     */
    public void start() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                logger.debug("Server started on port " + PORT);
                // Initialize the connectedClients counter
                int connectedClients = 0;
                // Keep accepting clients until 2 clients are connected
                while (connectedClients < 2) {
                    try {
                        // Accept a client connection
                        Socket socket = serverSocket.accept();
                        // Increment the connectedClients counter
                        connectedClients++;
                        logger.debug("Client connected (" + connectedClients + " total): " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
                        // Delegate client handling to a thread from the thread pool
                        executorService.execute(new ClientHandler(socket, philosopher));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                serverLatch.countDown(); // Count down the latch when the server finishes
            }
        }).start();
    }

    /**
     * Get the server latch
     *
     * @return The server latch
     */
    public CountDownLatch getServerLatch() {
        return serverLatch;
    }
}
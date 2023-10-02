import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

/**
 * A server is responsible for accepting client connections and delegating client handling to a thread from the thread pool
 */
public class Server {
    /**
     * The logger for the server class
     */
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    /**
     * The port that the server listens on
     */
    private final int PORT;
    /**
     * The philosopher that the server belongs to
     */
    private final Philosopher philosopher;
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
        this.philosopher = philosopher;
        this.PORT = port;
        this.serverLatch = new CountDownLatch(1); // Initialize the latch
    }

    /**
     * Start the server listener
     */
    public void startListener() {
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
                        // Create new thread for message handling per socket
                        messageHandler(socket, philosopher);
                    } catch (IOException e) {
                        logger.error("Error accepting client connection", e);
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
     * Handle messages from the client
     *
     * @param clientSocket The socket of the client
     * @param philosopher  The philosopher that the client handler belongs to
     */
    public void messageHandler(Socket clientSocket, Philosopher philosopher) {
        new Thread(() -> {
            try {
                ObjectInputStream in;
                while (true) {
                    try {
                        // Create an object input stream from the client socket
                        in = new ObjectInputStream(clientSocket.getInputStream());
                        // Read a message from the client
                        Message receivedMessage = (Message) in.readObject();
                        // Handle the message
                        if (receivedMessage.getType() == MessageType.REQUEST) {
                            philosopher.receiveRequest(clientSocket, receivedMessage);
                        } else if (receivedMessage.getType() == MessageType.REPLY) {
                            philosopher.receiveReply(receivedMessage.getPhilosopherId(), receivedMessage.getDirection());
                        } else if (receivedMessage.getType() == MessageType.COUNTER) {
                            philosopher.receiveCounter(receivedMessage.getPhilosopherId(), receivedMessage.getDirection(), receivedMessage.getGCounter());
                        } else if (receivedMessage.getType() == MessageType.S_PING) {
                            philosopher.receiveSPing(receivedMessage.getPhilosopherId(), receivedMessage.getDirection());
                        } else if (receivedMessage.getType() == MessageType.R_PING) {
                            philosopher.receiveRPing(receivedMessage.getPhilosopherId(), receivedMessage.getDirection());
                        }
                    } catch (EOFException e) {
                        logger.error("Error while handling client request", e);
                        break;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                logger.error("Error while handling client request", e);
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    logger.error("Error while closing client socket", e);
                }
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

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
        if (philosopher == null) throw new IllegalArgumentException("Philosopher cannot be null");
        if (port < 49152 || port > 65535)
            throw new InvalidPortException("Port is out of the valid range of 49152-65535");
        this.philosopher = philosopher;
        this.PORT = port;
        this.serverLatch = new CountDownLatch(1); // Initialize the latch
        startListener();
    }

    /**
     * Start the server listener
     */
    private void startListener() {
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
                        messageHandler(socket);
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
     */
    private void messageHandler(Socket clientSocket) {
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
                            receiveRequest(clientSocket, receivedMessage);
                        } else if (receivedMessage.getType() == MessageType.REPLY) {
                            receiveReply(receivedMessage.getPhilosopherId(), receivedMessage.getDirection());
                        } else if (receivedMessage.getType() == MessageType.COUNTER) {
                            receiveCounter(receivedMessage.getPhilosopherId(), receivedMessage.getDirection(), receivedMessage.getGCounter());
                        } else if (receivedMessage.getType() == MessageType.PING) {
                            receivePing(receivedMessage.getPhilosopherId(), receivedMessage.getHasReceivedPing(), receivedMessage.getDirection());
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
     * Receive a request from a neighbor
     *
     * @param requestingSocket The socket of the requesting neighbor
     * @param receivedMessage  The message received from the neighbor
     */
    private synchronized void receiveRequest(Socket requestingSocket, Message receivedMessage) {
        philosopher.setHasReply(true);
        // Get current timestamp
        int timestamp = philosopher.getLamportClock().getTimestamp();
        // On receiving a request, update the local Lamport timestamp
        philosopher.getLamportClock().synchronize(receivedMessage.getTimestamp());
        int requestTimestamp = receivedMessage.getTimestamp();
        Direction requestDirection = receivedMessage.getDirection();
        int requestPhilosopherId = receivedMessage.getPhilosopherId();

        // Site Sj is neither requesting nor currently executing the critical section send REPLY
        // In case Site Sj is requesting, the timestamp of Site Si's request is smaller than its own request send REPLY
        // In Case requestTimestamp == timestamp, Sj sends Si a REPLY
        // ELSE defer the request
        if ((!philosopher.inCriticalSection() && !philosopher.isRequesting()) || (philosopher.isRequesting() && (requestTimestamp < timestamp)) || (requestTimestamp == timestamp && requestPhilosopherId < philosopher.getPhilosopherId())) {
            logger.debug("Philosopher " + philosopher.getPhilosopherId() + " received REQUEST from Philosopher " + requestPhilosopherId + " " + requestDirection + " with timestamp " + requestTimestamp);
            if (requestDirection == Direction.LEFT) {
                philosopher.sendReply(philosopher.getLeftNeighborSocket(), philosopher.reverseDirection(requestDirection));
            } else {
                philosopher.sendReply(philosopher.getRightNeighborSocket(), philosopher.reverseDirection(requestDirection));
            }
        } else {
            // Defer the request
            logger.debug("Philosopher " + philosopher.getPhilosopherId() + " deferred REQUEST from Philosopher " + receivedMessage.getPhilosopherId() + " " + receivedMessage.getDirection());
            philosopher.getDeferredRequests().add(new DeferredRequest(requestingSocket, requestDirection));
        }
    }

    /**
     * Receive a reply from a neighbor
     *
     * @param clientId  The ID of the neighbor
     * @param direction The direction of the reply
     */
    private synchronized void receiveReply(int clientId, Direction direction) {
        if (direction == Direction.LEFT) {
            logger.debug("Philosopher " + philosopher.getPhilosopherId() + " received REPLY from Philosopher " + clientId + " " + direction);
            philosopher.setHasLeftFork(true);
        } else {
            logger.debug("Philosopher " + philosopher.getPhilosopherId() + " received REPLY from Philosopher " + clientId + " " + direction);
            philosopher.setHasRightFork(true);
        }
    }

    /**
     * Receive a counter from a neighbor
     *
     * @param clientId  The ID of the neighbor
     * @param direction The direction of the counter
     * @param gCounter  The counter object of the philosopher
     */
    private synchronized void receiveCounter(int clientId, Direction direction, GCounter gCounter) {
        logger.debug("Philosopher " + philosopher.getPhilosopherId() + " received COUNTER from Philosopher " + clientId + " " + direction);
        philosopher.getLocalCounter().merge(gCounter);
    }

    /**
     * Receive a ping from a neighbor
     *
     * @param clientId  The ID of the neighbor
     * @param direction The direction of the ping
     */
    private synchronized void receivePing(int clientId, boolean hasReceivedPing, Direction direction) {
        logger.debug("Philosopher " + philosopher.getPhilosopherId() + " received PING from Philosopher " + clientId + " " + direction);
        if (direction == Direction.LEFT && !hasReceivedPing) {
            philosopher.sendPing(philosopher.getLeftNeighborSocket(), true, philosopher.reverseDirection(direction));
        } else if (direction == Direction.RIGHT && !hasReceivedPing) {
            philosopher.sendPing(philosopher.getRightNeighborSocket(), true, philosopher.reverseDirection(direction));
        } else if (direction == Direction.LEFT) {
            philosopher.setReceivedPingLeft(true);
        } else if (direction == Direction.RIGHT) {
            philosopher.setReceivedPingRight(true);
        }
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

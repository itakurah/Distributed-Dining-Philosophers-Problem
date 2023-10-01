import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The Philosopher class represents a philosopher in the dining philosophers problem
 */
class Philosopher {
    /**
     * The logger for the Philosopher class
     */
    private static final Logger logger = LoggerFactory.getLogger(Philosopher.class);
    /**
     * The interval for eating
     * The first element is the maximum time in milliseconds
     * The second element is the minimum time in milliseconds
     */
    private final int[] eatInterval = new int[]{10000, 5000};
    /**
     * The interval for thinking
     * The first element is the maximum time in milliseconds
     * The second element is the minimum time in milliseconds
     */
    private final int[] thinkInterval = new int[]{30000, 5000};
    /**
     * The maximum number of retries
     */
    int maxRetries = 20;
    /**
     * The interval between retries in milliseconds
     */
    int retryIntervalMillis = 1000;
    /**
     * The ID of the philosopher
     */
    private int philosopherId;
    /**
     * The Lamport clock of the philosopher
     */
    private final LamportClock lamportClock = new LamportClock();
    /**
     * The left fork of the philosopher
     */
    private boolean hasLeftFork;
    private boolean hasRightFork;
    /**
     * The socket of the left and right neighbors
     */
    private Socket leftNeighborSocket;
    private Socket rightNeighborSocket;
    /**
     * The state of the philosopher
     */
    private boolean inCriticalSection;
    private boolean isRequesting;
    /**
     * The queue of deferred requests
     */
    private final BlockingQueue<DeferredRequest> deferredRequests = new LinkedBlockingQueue<>(2);
    /**
     * The local counter of the philosopher
     */
    private final GCounter localCounter = new GCounter(philosopherId);

    /**
     * The interval between updates sent to neighbors in milliseconds
     */
    private final int UPDATE_INTERVAL = 1000;

    /**
     * Constructor for the Philosopher class
     *
     * @param philosopherId The ID of the philosopher
     */
    public Philosopher(int philosopherId) {
        this.philosopherId = philosopherId;
        this.hasLeftFork = false;
        this.hasRightFork = false;
        this.inCriticalSection = false;
        this.isRequesting = false;
    }

    /**
     * Simulate thinking
     */
    public void think() {
        logger.info("Philosopher " + philosopherId + " is thinking.");
        try {
            Thread.sleep(new Random().nextInt(thinkInterval[0] - thinkInterval[1] + 1) + thinkInterval[1]);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Simulate eating
     */
    public void eat() {
        // Increment the local counter
        localCounter.increment();
        logger.info("Philosopher " + philosopherId + " is eating.");
        try {
            Thread.sleep(new Random().nextInt(eatInterval[0] - eatInterval[1] + 1) + eatInterval[1]);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request forks from neighbors
     */
    public void requestForks() {
        // On request, update the Lamport timestamp
        lamportClock.update();
        // Requesting forks
        setRequesting(true);
        // Get the current Lamport timestamp
        int timestamp = lamportClock.getTimestamp();
        logger.debug("Philosopher " + philosopherId + " is requesting forks with timestamp " + timestamp);
        logger.info("Philosopher " + philosopherId + " is requesting forks.");
        // Request forks from neighbors
        logger.info("Philosopher " + philosopherId + " is requesting left fork.");
        sendRequest(leftNeighborSocket, Direction.RIGHT, timestamp);
        logger.info("Philosopher " + philosopherId + " is requesting right fork.");
        sendRequest(rightNeighborSocket, Direction.LEFT, timestamp);
        // Print messages
        boolean printedLeftForkMessage = false;
        boolean printedRightForkMessage = false;
        // Wait until both forks are acquired
        while (!(hasLeftFork && hasRightFork)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (hasLeftFork && !printedLeftForkMessage) {
                logger.info("Philosopher " + philosopherId + " has left fork.");
                printedLeftForkMessage = true;
            }

            if (hasRightFork && !printedRightForkMessage) {
                logger.info("Philosopher " + philosopherId + " has right fork.");
                printedRightForkMessage = true;
            }
        }
        // Enter critical section
        setInCriticalSection(true);
        // No longer requesting forks
        setRequesting(false);
        logger.debug("Philosopher " + philosopherId + " entered the critical section.");

    }

    /**
     * Release forks to neighbors
     */
    public void releaseForks() {
        // Exit critical section
        setInCriticalSection(false);
        logger.debug("Philosopher " + philosopherId + " is releasing forks.");
        logger.info("Philosopher " + philosopherId + " is releasing forks.");
        logger.debug("deferredRequests: " + deferredRequests.size());
        // Release forks to neighbors
        while (!deferredRequests.isEmpty()) {
            DeferredRequest request = deferredRequests.poll();
            logger.debug(String.valueOf(request));
            if (request.getDirection() == Direction.LEFT) {
                sendReply(leftNeighborSocket, reverseDirection(request.getDirection()));
            } else {
                sendReply(rightNeighborSocket, reverseDirection(request.getDirection()));
            }
        }
        // Reset fork states
        hasLeftFork = false;
        hasRightFork = false;
        logger.info("Philosophers have eaten a total of " + localCounter.query() + " times");
    }

    /**
     * Reverse the direction of the request
     *
     * @param direction The direction of the request
     * @return The reversed direction
     */
    public Direction reverseDirection(Direction direction) {
        if (direction == Direction.LEFT) {
            return Direction.RIGHT;
        } else {
            return Direction.LEFT;
        }
    }

    /**
     * Send a request to a neighbor
     *
     * @param receivingSocket The socket of the receiving neighbor
     * @param direction       The direction of the request
     * @param timestamp       The timestamp of the request
     */
    private void sendRequest(Socket receivingSocket, Direction direction, int timestamp) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(receivingSocket.getOutputStream());
            Message requestMessage = new Message(MessageType.REQUEST, this.philosopherId, direction, timestamp);
            out.writeObject(requestMessage);
            //out.flush(); hotfix for java.net.SocketException: Connection reset
            logger.debug("Philosopher " + philosopherId + " sent REQUEST to Philosopher " + reverseDirection(direction) + " with timestamp " + timestamp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Receive a request from a neighbor
     *
     * @param requestingSocket The socket of the requesting neighbor
     * @param receivedMessage  The message received from the neighbor
     */
    public void receiveRequest(Socket requestingSocket, Message receivedMessage) {
        // Get current timestamp
        int timestamp = lamportClock.getTimestamp();
        // On receiving a request, update the local Lamport timestamp
        lamportClock.synchronize(receivedMessage.getTimestamp());
        int requestTimestamp = receivedMessage.getTimestamp();
        Direction requestDirection = receivedMessage.getDirection();
        int requestPhilosopherId = receivedMessage.getPhilosopherId();

        // Site Sj is neither requesting nor currently executing the critical section send REPLY
        // In case Site Sj is requesting, the timestamp of Site Si's request is smaller than its own request send REPLY
        // In Case requestTimestamp == timestamp, Sj sends Si a REPLY
        // ELSE defer the request
        if ((!inCriticalSection() && !isRequesting()) || (isRequesting() && (requestTimestamp < timestamp)) || (requestTimestamp == timestamp && requestPhilosopherId < philosopherId)) {
            logger.debug("Philosopher " + philosopherId + " received REQUEST from Philosopher " + requestPhilosopherId + " " + requestDirection + " with timestamp " + requestTimestamp);
            if (requestDirection == Direction.LEFT) {
                sendReply(leftNeighborSocket, reverseDirection(requestDirection));
            } else {
                sendReply(rightNeighborSocket, reverseDirection(requestDirection));
            }
        } else {
            // Defer the request
            logger.debug("Philosopher " + philosopherId + " deferred REQUEST from Philosopher " + receivedMessage.getPhilosopherId() + " " + receivedMessage.getDirection());
            deferredRequests.add(new DeferredRequest(requestingSocket, requestTimestamp, requestDirection));
        }
    }

    /**
     * Send a reply to a neighbor
     *
     * @param receivingSocket The socket of the receiving neighbor
     * @param direction       The direction of the reply
     */
    private void sendReply(Socket receivingSocket, Direction direction) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(receivingSocket.getOutputStream());
            Message replyMessage = new Message(MessageType.REPLY, this.philosopherId, direction);
            out.writeObject(replyMessage);
            //out.flush(); hotfix for java.net.SocketException: Connection reset
            logger.debug("Philosopher " + philosopherId + " sent REPLY to Philosopher " + reverseDirection(direction));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Receive a reply from a neighbor
     *
     * @param clientId  The ID of the neighbor
     * @param direction The direction of the reply
     */
    public void receiveReply(int clientId, Direction direction) {
        if (direction == Direction.LEFT) {
            logger.debug("Philosopher " + philosopherId + " received REPLY from Philosopher " + clientId + " " + direction);
            hasLeftFork = true;
        } else {
            logger.debug("Philosopher " + philosopherId + " received REPLY from Philosopher " + clientId + " " + direction);
            hasRightFork = true;
        }
    }

    /**
     * Send a counter to a neighbor
     *
     * @param receivingSocket The socket of the receiving neighbor
     * @param direction       The direction of the reply
     */
    private void sendCounter(Socket receivingSocket, Direction direction, GCounter gCounter) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(receivingSocket.getOutputStream());
            Message counterMessage = new Message(MessageType.COUNTER, this.philosopherId, direction, gCounter);
            out.writeObject(counterMessage);
            //out.flush(); hotfix for java.net.SocketException: Connection reset
            logger.debug("Philosopher " + philosopherId + " sent COUNTER to Philosopher " + reverseDirection(direction));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the neighbor counter every 1 second
     */
    public void updateNeighborCounter(){
        // Send the counter to the neighbors
        try {
            Thread.sleep(UPDATE_INTERVAL);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        sendCounter(rightNeighborSocket, Direction.LEFT, localCounter);
        sendCounter(leftNeighborSocket, Direction.RIGHT, localCounter);
    }

    /**
     * Receive a counter from a neighbor
     *
     * @param clientId  The ID of the neighbor
     * @param direction The direction of the counter
     * @param gCounter  The counter object of the philosopher
     */
    public void receiveCounter(int clientId, Direction direction, GCounter gCounter) {
        logger.debug("Philosopher " + philosopherId + " received COUNTER from Philosopher " + clientId + " " + direction);
        localCounter.merge(gCounter);
    }

    /**
     * Connect to the left neighbor
     *
     * @param leftNeighborAddress The address of the left neighbor
     */
    public void connectToLeftNeighbor(InetSocketAddress leftNeighborAddress) {
        int maxRetries = 5;
        int retryIntervalMillis = 2000; // 5 seconds
        for (int retryCount = 1; retryCount <= maxRetries; retryCount++) {
            try {
                leftNeighborSocket = new Socket(leftNeighborAddress.getAddress(), leftNeighborAddress.getPort());
                logger.debug("Connected to neighbor: " + leftNeighborSocket);
                break;
            } catch (IOException e) {
                // Print the error and retry after the interval
                logger.debug("Could not connect to neighbors");
                if (retryCount < maxRetries) {
                    logger.debug("Retrying in " + retryIntervalMillis / 1000 + " seconds...");
                    try {
                        Thread.sleep(retryIntervalMillis);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    logger.error("Failed to connect after " + maxRetries + " retries.");
                    System.exit(1);
                }
            }
        }
    }

    /**
     * Connect to the right neighbor
     *
     * @param rightNeighborAddress The address of the right neighbor
     */
    public void connectToRightNeighbor(InetSocketAddress rightNeighborAddress) {
        for (int retryCount = 1; retryCount <= maxRetries; retryCount++) {
            try {
                rightNeighborSocket = new Socket(rightNeighborAddress.getAddress(), rightNeighborAddress.getPort());
                logger.debug("Connected to neighbor: " + rightNeighborSocket);
                break;
            } catch (IOException e) {
                // Print the error and retry after the interval
                logger.debug("Could not connect to neighbors");
                //e.printStackTrace();
                if (retryCount < maxRetries) {
                    logger.debug("Retrying in " + retryIntervalMillis / 1000 + " seconds...");
                    try {
                        Thread.sleep(retryIntervalMillis);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    logger.error("Failed to connect after " + maxRetries + " retries.");
                    System.exit(1);
                }
            }
        }
    }

    /**
     * Check if the philosopher is in the critical section
     *
     * @return True if the philosopher is in the critical section, false otherwise
     */
    public synchronized boolean inCriticalSection() {
        return inCriticalSection;
    }

    /**
     * Set the philosopher to be in the critical section
     *
     * @param inCriticalSection True if the philosopher is in the critical section, false otherwise
     */
    public synchronized void setInCriticalSection(boolean inCriticalSection) {
        this.inCriticalSection = inCriticalSection;
    }

    /**
     * Check if the philosopher is requesting forks
     *
     * @return True if the philosopher is requesting forks, false otherwise
     */
    public synchronized boolean isRequesting() {
        return isRequesting;
    }

    /**
     * Set the philosopher to be requesting forks
     *
     * @param requesting True if the philosopher is requesting forks, false otherwise
     */
    public synchronized void setRequesting(boolean requesting) {
        isRequesting = requesting;
    }
}
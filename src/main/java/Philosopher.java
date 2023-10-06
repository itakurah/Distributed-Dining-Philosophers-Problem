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
public class Philosopher {
    /**
     * The logger for the Philosopher class
     */
    private static final Logger logger = LoggerFactory.getLogger(Philosopher.class);
    /**
     * The maximum number of retries
     */
    final int NUM_OF_RETRIES = 20;
    /**
     * The interval between retries in milliseconds
     */
    final int RETRY_INTERVAL = 2000;
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
     * The Lamport clock of the philosopher
     */
    private final LamportClock lamportClock = new LamportClock();
    /**
     * The queue of deferred requests
     */
    private final BlockingQueue<DeferredRequest> deferredRequests = new LinkedBlockingQueue<>(2);
    /**
     * The interval between updates sent to neighbors in milliseconds
     */
    private final int UPDATE_INTERVAL = 100;
    private final int PING_INTERVAL = 5000;
    /**
     * The ID of the philosopher
     */
    private final int philosopherId;
    /**
     * The local counter of the philosopher
     */
    private final GCounter localGCounter;
    private final boolean isTest = false;
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
     * The state of the ping
     */
    private boolean receivedPingLeft = false;
    private boolean receivedPingRight = false;
    /**
     * The state of the reply
     */
    private boolean hasReply = true;

    /**
     * Constructor for the Philosopher class
     *
     * @param philosopherId The ID of the philosopher
     */
    public Philosopher(int philosopherId, String leftNeighborAddress, int leftNeighborPort, String rightNeighborAddress, int rightNeighborPort) {
        if (philosopherId <= 0) throw new IllegalArgumentException("Philosopher ID must be greater than 0");
        if (leftNeighborAddress == null) throw new IllegalArgumentException("Left neighbor address cannot be null");
        if (rightNeighborAddress == null) throw new IllegalArgumentException("Right neighbor address cannot be null");
        if ((leftNeighborPort < 49152 || rightNeighborPort < 49152) || (leftNeighborPort > 65535 | rightNeighborPort > 65535))
            throw new IllegalArgumentException("Port is out of the valid range of 49152-65535");
        this.philosopherId = philosopherId;
        this.hasLeftFork = false;
        this.hasRightFork = false;
        this.inCriticalSection = false;
        this.isRequesting = false;
        this.localGCounter = new GCounter(philosopherId);
        // Connect to left and right neighbors
        if (!isTest) {
            logger.debug("Connecting to neighbors");
            connectToNeighbor(new InetSocketAddress(leftNeighborAddress, leftNeighborPort), Direction.LEFT);
            connectToNeighbor(new InetSocketAddress(rightNeighborAddress, rightNeighborPort), Direction.RIGHT);
            logger.debug("Connected to neighbors");
        }

    }

    /**
     * Simulate thinking
     */
    public void think() {
        logger.info("Philosopher " + philosopherId + " is thinking.");
        try {
            Thread.sleep(new Random().nextInt(thinkInterval[0] - thinkInterval[1] + 1) + thinkInterval[1]);
        } catch (InterruptedException e) {
            logger.error("An error occurred while thinking", e);
        }
    }

    /**
     * Simulate eating
     */
    public void eat() {
        // Increment the local counter
        localGCounter.increment();
        logger.info("Philosopher " + philosopherId + " is eating.");
        try {
            Thread.sleep(new Random().nextInt(eatInterval[0] - eatInterval[1] + 1) + eatInterval[1]);
        } catch (InterruptedException e) {
            logger.error("An error occurred while eating", e);
        }
    }

    /**
     * Request forks from neighbors
     */
    public void requestForks() {
        // On request, update the Lamport timestamp
        lamportClock.update();
        // Roucairol-Carvalho optimization
        // Check if the philosopher has received a reply from both neighbors
        // Once site Pi has received a reply message from site Pj, site Pi may enter
        // the critical section multiple times without receiving permission from Pj on
        // subsequent attempts up to the moment when Pi has sent a reply message to Pj.
        if (hasReply()) {
            setHasReply(false);
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
                    logger.error("An error occurred while waiting for forks", e);
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
        logger.info("Philosophers have eaten a total of " + localGCounter.query() + " times");
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
     * Send a ping to neighbors
     */
    public void requestPing() {
        new Thread(() -> {
            while (true) {
                try {
                    sendPing(leftNeighborSocket, false, Direction.RIGHT);
                    sendPing(rightNeighborSocket, false, Direction.LEFT);
                    Thread.sleep(PING_INTERVAL);
                    if (!(isReceivedPingLeft() && isReceivedPingRight())) {
                        logger.error("Philosopher " + philosopherId + " has not received a ping back from his neighbors");
                        logger.error("Philosopher " + philosopherId + " left the table");
                        System.exit(0);
                    }
                    setReceivedPingLeft(false);
                    setReceivedPingRight(false);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    /**
     * Send a request to a neighbor
     *
     * @param receivingSocket The socket of the receiving neighbor
     * @param direction       The direction of the request
     * @param timestamp       The timestamp of the request
     */
    private synchronized void sendRequest(Socket receivingSocket, Direction direction, int timestamp) {
        ObjectOutputStream out;
        try {
            out = new ObjectOutputStream(receivingSocket.getOutputStream());
            Message requestMessage = new Message(MessageType.REQUEST, this.philosopherId, direction, timestamp);
            out.writeObject(requestMessage);
            //out.flush(); hotfix for java.net.SocketException: Connection reset
            logger.debug("Philosopher " + philosopherId + " sent REQUEST to Philosopher " + reverseDirection(direction) + " with timestamp " + timestamp);

        } catch (IOException e) {
            logger.error("An error occurred while sending a request", e);
        }
    }

    /**
     * Send a reply to a neighbor
     *
     * @param receivingSocket The socket of the receiving neighbor
     * @param direction       The direction of the reply
     */
    public synchronized void sendReply(Socket receivingSocket, Direction direction) {
        ObjectOutputStream out;
        try {
            out = new ObjectOutputStream(receivingSocket.getOutputStream());
            Message replyMessage = new Message(MessageType.REPLY, this.philosopherId, direction);
            out.writeObject(replyMessage);
            //out.flush(); hotfix for java.net.SocketException: Connection reset
            logger.debug("Philosopher " + philosopherId + " sent REPLY to Philosopher " + reverseDirection(direction));
        } catch (IOException e) {
            logger.error("An error occurred while sending a reply", e);
        }
    }

    /**
     * Send a counter to a neighbor
     *
     * @param receivingSocket The socket of the receiving neighbor
     * @param direction       The direction of the reply
     */
    public synchronized void sendCounter(Socket receivingSocket, Direction direction, GCounter gCounter) {
        ObjectOutputStream out;
        try {
            out = new ObjectOutputStream(receivingSocket.getOutputStream());
            Message counterMessage = new Message(MessageType.COUNTER, this.philosopherId, direction, gCounter);
            out.writeObject(counterMessage);
            //out.flush(); hotfix for java.net.SocketException: Connection reset
            logger.debug("Philosopher " + philosopherId + " sent COUNTER to Philosopher " + reverseDirection(direction));

        } catch (IOException e) {
            logger.error("An error occurred while sending a counter", e);
        }
    }

    /**
     * Update the neighbor G-Counter
     */
    public void updateNeighborCounter() {
        new Thread(() -> {
            while (true) {
                // Send the counter to the neighbors
                try {
                    Thread.sleep(UPDATE_INTERVAL);
                    sendCounter(rightNeighborSocket, Direction.LEFT, localGCounter);
                    sendCounter(leftNeighborSocket, Direction.RIGHT, localGCounter);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    /**
     * Send a ping to a neighbor
     *
     * @param receivingSocket The socket of the receiving neighbor
     * @param hasReceivedPing The hasReceivedPing flag of the sending philosopher
     * @param direction       The direction of the ping
     */
    public synchronized void sendPing(Socket receivingSocket, boolean hasReceivedPing, Direction direction) {
        ObjectOutputStream out;
        try {
            out = new ObjectOutputStream(receivingSocket.getOutputStream());
            Message replyMessage = new Message(MessageType.PING, this.philosopherId, hasReceivedPing, direction);
            out.writeObject(replyMessage);
            //out.flush(); hotfix for java.net.SocketException: Connection reset
            logger.debug("Philosopher " + philosopherId + " sent PING to Philosopher " + reverseDirection(direction));

        } catch (IOException e) {
            logger.error("An error occurred while sending a ping", e);
        }
    }


    /**
     * Connect to a neighbor
     *
     * @param neighborAddress The address of a neighbor
     */
    private void connectToNeighbor(InetSocketAddress neighborAddress, Direction direction) {
        new Thread(() -> {
            for (int retryCount = 1; retryCount <= NUM_OF_RETRIES; retryCount++) {
                try {
                    if (direction == Direction.LEFT) {
                        leftNeighborSocket = new Socket(neighborAddress.getAddress(), neighborAddress.getPort());
                        logger.debug("Connected to neighbor: " + leftNeighborSocket);
                    } else {
                        rightNeighborSocket = new Socket(neighborAddress.getAddress(), neighborAddress.getPort());
                        logger.debug("Connected to neighbor: " + rightNeighborSocket);
                    }
                    break;
                } catch (IOException e) {
                    // Print the error and retry after the interval
                    logger.debug("Could not connect to neighbors");
                    if (retryCount < NUM_OF_RETRIES) {
                        logger.debug("Retrying in " + RETRY_INTERVAL / 1000 + " seconds...");
                        try {
                            Thread.sleep(RETRY_INTERVAL);
                        } catch (InterruptedException ex) {
                            logger.error("Thread interrupted while connecting to the neighbor", ex);
                        }
                    } else {
                        logger.error("Failed to connect after " + NUM_OF_RETRIES + " retries.");
                        System.exit(1);
                    }
                }
            }
        }).start();
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
    private synchronized void setInCriticalSection(boolean inCriticalSection) {
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

    public synchronized boolean isReceivedPingLeft() {
        return receivedPingLeft;
    }

    public synchronized void setReceivedPingLeft(boolean receivedPingLeft) {
        this.receivedPingLeft = receivedPingLeft;
    }

    public synchronized boolean isReceivedPingRight() {
        return receivedPingRight;
    }

    public synchronized void setReceivedPingRight(boolean receivedPingRight) {
        this.receivedPingRight = receivedPingRight;
    }

    public LamportClock getLamportClock() {
        return lamportClock;
    }

    public int getPhilosopherId() {
        return philosopherId;
    }

    public Socket getLeftNeighborSocket() {
        return leftNeighborSocket;
    }

    public Socket getRightNeighborSocket() {
        return rightNeighborSocket;
    }

    public BlockingQueue<DeferredRequest> getDeferredRequests() {
        return deferredRequests;
    }

    public void setHasLeftFork(boolean hasLeftFork) {
        this.hasLeftFork = hasLeftFork;
    }

    public void setHasRightFork(boolean hasRightFork) {
        this.hasRightFork = hasRightFork;
    }

    public boolean hasLeftFork() {
        return hasLeftFork;
    }

    public boolean hasRightFork() {
        return hasRightFork;
    }

    public GCounter getLocalGCounter() {
        return localGCounter;
    }

    public synchronized boolean hasReply() {
        return hasReply;
    }

    public synchronized void setHasReply(boolean hasReply) {
        this.hasReply = hasReply;
    }
}
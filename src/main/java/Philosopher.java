import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class Philosopher {
    private int philosopherId;
    private int timestamp;
    private LamportClock lamportClock = new LamportClock();
    private boolean hasLeftFork;
    private boolean hasRightFork;
    private Socket leftNeighborSocket;
    private Socket rightNeighborSocket;
    private boolean inCriticalSection;
    private boolean isRequesting;
    //private BlockingQueue<DeferredRequest> messageQueue = new LinkedBlockingQueue<>();
    private static final Logger logger = LogManager.getLogger(Philosopher.class);
    //private List<DeferredRequest> deferredRequests = new ArrayList<>();
    private BlockingQueue<DeferredRequest> deferredRequests = new LinkedBlockingQueue<>(2);

    private final Object lock = new Object();

    public Philosopher(int philosopherId) {
        this.philosopherId = philosopherId;
        this.timestamp = 0;
        this.hasLeftFork = false;
        this.hasRightFork = false;
        this.inCriticalSection = false;
        this.isRequesting = false;
    }

    public void think() {
        logger.info("Philosopher " + philosopherId + " is thinking.");
        try {
            int rnd = new Random().nextInt(20000 - 5000 + 1) + 5000;
            System.out.println(rnd);
            Thread.sleep(rnd); // Simulate thinking
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void eat() {
        logger.info("Philosopher " + philosopherId + " is eating.");
        try {
            Thread.sleep(new Random().nextInt(10000 - 5000 + 1) + 5000); // Simulate eating
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void requestForks() {
        //timestamp++;
        lamportClock.update();
        synchronized (lock) {
            isRequesting = true;
        }

        logger.debug("Philosopher " + philosopherId + " is requesting forks with timestamp " + timestamp);

        // Request forks from neighbors
        sendRequest(leftNeighborSocket, "right");
        sendRequest(rightNeighborSocket, "left");

        // Wait until both forks are acquired
        while (!(hasLeftFork && hasRightFork)) {
            try {
                logger.debug("waiting for philosophers");
                logger.debug("hasForkleft " + hasLeftFork);
                logger.debug("hasForkright " + hasRightFork);
                Thread.sleep(1000); // Simulate waiting for forks
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        synchronized (lock) {
            inCriticalSection = true;
            isRequesting = false;
        }
        logger.debug("left" + hasLeftFork);
        logger.debug("right" + hasRightFork);
        logger.debug("Philosopher " + philosopherId + " entered the critical section.");

    }

    public void releaseForks() {
        synchronized (lock) {
            inCriticalSection = false;
        }
        logger.debug("Philosopher " + philosopherId + " is releasing forks.");

        // Release forks to neighbors
        //sendReply(leftNeighborSocket, "right");
        //sendReply(rightNeighborSocket, "left");
        System.out.println(deferredRequests.size());
//        for (DeferredRequest deferredClient : deferredRequests
//        ) {
//            System.out.println(deferredClient);
//            sendReply(deferredClient.getSocket(), deferredClient.getDirection());
//        }

//        while (!deferredRequests.isEmpty()) {
//            DeferredRequest request = deferredRequests.poll(); // This blocks until an element is available.
//            System.out.println(request);
//            sendReply(request.getSocket(), request.getDirection());
//        }
        while (!deferredRequests.isEmpty()) {
            DeferredRequest request = deferredRequests.poll();
            System.out.println(request);
            if (request.getDirection().equals("left")) {
                sendReply(leftNeighborSocket, reverseDirection(request.getDirection()));
            } else {
                sendReply(rightNeighborSocket, reverseDirection(request.getDirection()));
            }
        }
        //deferredRequests.clear();
        hasLeftFork = false;
        hasRightFork = false;
    }

    public synchronized String reverseDirection(String direction) {
        if (direction.equals("left")) {
            return "right";
        } else {
            return "left";
        }
    }

    public synchronized void receiveRequest(Socket requestingSite, Message receivedMessage) {
        lamportClock.synchronize(receivedMessage.getLamportClock().getTimestamp());
        synchronized (lock) {
            if ((!inCriticalSection && !isRequesting) || (isRequesting && (receivedMessage.getLamportClock().getTimestamp() < timestamp)) || (receivedMessage.getLamportClock().getTimestamp() == timestamp && receivedMessage.getPhilosopherId() < philosopherId)) {
                // Philosopher can reply to the request
                logger.debug("Philosopher " + philosopherId + " received REQUEST from Philosopher " + receivedMessage.getPhilosopherId() + " " + receivedMessage.getDirection());
                System.out.println((!inCriticalSection && !isRequesting));
                System.out.println((isRequesting && (receivedMessage.getLamportClock().getTimestamp() < timestamp)));
                System.out.println((receivedMessage.getLamportClock().getTimestamp() == timestamp && receivedMessage.getPhilosopherId() < philosopherId));
                if (receivedMessage.getDirection().equals("left")) {
                    sendReply(leftNeighborSocket, reverseDirection(receivedMessage.getDirection()));
                } else {
                    sendReply(rightNeighborSocket, reverseDirection(receivedMessage.getDirection()));
                }
            } else {
                // Philosopher defers the request
                logger.debug("Philosopher " + philosopherId + " deferred REQUEST from Philosopher " + receivedMessage.getPhilosopherId() + " " + receivedMessage.getDirection());
                deferredRequests.add(new DeferredRequest(requestingSite, receivedMessage.getLamportClock().getTimestamp(), receivedMessage.getDirection()));
            }
        }
    }

    public synchronized void receiveReply(int clientId, String direction) {
        if (direction.equals("left")) {
            logger.debug("Philosopher " + philosopherId + " received REPLY from Philosopher " + clientId + " " + direction);
            hasLeftFork = true;
        } else {
            logger.debug("Philosopher " + philosopherId + " received REPLY from Philosopher " + clientId + " " + direction);
            hasRightFork = true;
        }
    }

    private synchronized void sendReply(Socket receivingSite, String direction) {
        //timestamp++;
        try {
            ObjectOutputStream out = new ObjectOutputStream(receivingSite.getOutputStream());

            Message replyMessage = new Message(MessageType.REPLY, this.philosopherId, direction, lamportClock);
            out.writeObject(replyMessage);
            //out.flush(); hotfix for java.net.SocketException: Connection reset
            logger.debug("Philosopher " + philosopherId + " sent REPLY to Philosopher " + reverseDirection(direction));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void sendRequest(Socket receivingSite, String direction) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(receivingSite.getOutputStream());

            Message requestMessage = new Message(MessageType.REQUEST, this.philosopherId, direction, lamportClock);
            out.writeObject(requestMessage);
            //out.flush(); hotfix for java.net.SocketException: Connection reset
            logger.debug("Philosopher " + philosopherId + " sent REQUEST to Philosopher " + reverseDirection(direction));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


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

    public void connectToRightNeighbor(InetSocketAddress rightNeighborAddress) {
        int maxRetries = 5;
        int retryIntervalMillis = 2000; // 5 seconds
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
}
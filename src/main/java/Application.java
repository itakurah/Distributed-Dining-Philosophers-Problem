import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Main class to be run with docker
 */
public class Application {
    /**
     * The logger for the Application class
     */
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        InetSocketAddress leftNeighborAddress = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
        InetSocketAddress rightNeighborAddress = new InetSocketAddress(args[4], Integer.parseInt(args[5]));
        Philosopher philosopher = new Philosopher(Integer.parseInt(args[0]));
        Server server = new Server(philosopher, Integer.parseInt(args[1]));
        // Start server listener
        logger.debug("Starting server listener");
        server.startListener();
        // Connect to left and right neighbors
        logger.debug("Connecting to neighbors");
        philosopher.connectToNeighbor(leftNeighborAddress, Direction.LEFT);
        philosopher.connectToNeighbor(rightNeighborAddress, Direction.RIGHT);
        logger.debug("Connected to neighbors");
        try {
            // Wait for the server to finish
            server.getServerLatch().await();
            // used to keep the container running in docker
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            logger.error("Error while waiting for server to finish", e);
        }
        // Start game thread
        logger.debug("Starting game thread");
        Game game = new Game(philosopher);
        game.start();
        // Start neighbor counter
        philosopher.updateNeighborCounter();
        // Start ping service
        philosopher.requestPing();
    }
}

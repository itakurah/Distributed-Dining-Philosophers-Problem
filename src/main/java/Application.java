import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main class to be run with docker
 */
public class Application {
    /**
     * The logger for the Application class
     */
    private static final Logger logger = LogManager.getLogger(Server.class);
    private final Level NOTICE = Level.forName("NOTICE", 350);

    public static void main(String[] args) {
        Philosopher philosopher = new Philosopher(Integer.parseInt(args[0]), args[2], Integer.parseInt(args[3]), args[4], Integer.parseInt(args[5]));
        Server server = new Server(philosopher, Integer.parseInt(args[1]));
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
        logger.debug("Starting counter thread");
        // Start neighbor counter
        philosopher.updateNeighborCounter();
        logger.debug("Starting ping thread");
        // Start ping service
        philosopher.requestPing();
    }
}

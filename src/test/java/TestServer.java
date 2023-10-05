import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestServer {
    /**
     * Test if server exceptions are thrown when invalid philosophers are used
     */
    @Test
    void serverInvalidPhilosopherException() {
        assertThrows(IllegalArgumentException.class, () -> new Server(null, 49152));
    }

    /**
     * Test if server exceptions are thrown when invalid ports are used
     */

    @Test
    void serverInvalidPortException() {
        Philosopher philosopher1 = new Philosopher(1, "localhost", 49154, "localhost", 49154);
        Philosopher philosopher2 = new Philosopher(2, "localhost", 49155, "localhost", 49155);

        assertThrows(IllegalArgumentException.class, () -> new Server(philosopher1, 49151));
        assertThrows(IllegalArgumentException.class, () -> new Server(philosopher2, 65536));
    }

    /**
     * Test if client connections are accepted
     */
    @Test
    void serverAllNeighborsAreConnected() {
        Philosopher philosopher1 = new Philosopher(1, "localhost", 49156, "localhost", 49156);
        Philosopher philosopher2 = new Philosopher(2, "localhost", 49157, "localhost", 49157);

        Server server1 = new Server(philosopher1, 49157);
        Server server2 = new Server(philosopher2, 49156);
        try {
            // Wait for the server to finish
            server1.getServerLatch().await();
            server2.getServerLatch().await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Error while waiting for server to finish", e);
        }

        Assertions.assertTrue(areNeighborsConnected(server1));
        Assertions.assertTrue(areNeighborsConnected(server2));
    }

    /**
     * Util method to check if the neighbors of a server are connected
     */
    private boolean areNeighborsConnected(Server server) {
        try {
            Field philosopherField = server.getClass().getDeclaredField("philosopher");
            philosopherField.setAccessible(true);
            Object philosopherObject = philosopherField.get(server);

            if (philosopherObject instanceof Philosopher philosopher) {
                return philosopher.getLeftNeighborSocket().isConnected() &&
                        philosopher.getRightNeighborSocket().isConnected();
            }
        } catch (Exception e) {
            throw new RuntimeException("Reflection failed", e);
        }
        return false;
    }
}

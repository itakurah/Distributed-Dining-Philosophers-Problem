import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertThrows;


public class TestServer {
    private Philosopher philosopher1;
    private Philosopher philosopher2;


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
        assertThrows(IllegalArgumentException.class, () -> new Server(new Philosopher(1, "localhost", 49170, "localhost", 49170), 49151));
        assertThrows(IllegalArgumentException.class, () -> new Server(new Philosopher(2, "localhost", 49171, "localhost", 49171), 65536));
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

    @Test
    void serverIsPhilosopherLamportClockWorkingForTwoPhilosophers() {
        Philosopher philosopher1 = new Philosopher(1, "localhost", 49158, "localhost", 49158);
        Philosopher philosopher2 = new Philosopher(2, "localhost", 49159, "localhost", 49159);
        // Get the Class object for the Philosopher class
        Class<?> philosopherClass = Philosopher.class;

        // Get the Field object for the eatInterval field
        Field eatIntervalField = null;
        try {
            eatIntervalField = philosopherClass.getDeclaredField("eatInterval");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        // Make the field accessible (since it's private)
        eatIntervalField.setAccessible(true);

        // Create an array to represent the new eatInterval value
        int[] newEatInterval = new int[]{100, 50};

        // Set the new value for eatInterval
        try {
            eatIntervalField.set(philosopher1, newEatInterval);
            eatIntervalField.set(philosopher2, newEatInterval);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        Server server1 = new Server(philosopher1, 49159);
        Server server2 = new Server(philosopher2, 49158);
        try {
            // Wait for the server to finish
            server1.getServerLatch().await();
            server2.getServerLatch().await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Error while waiting for server to finish", e);
        }
        for (int i = 0; i < 2; i++) {
            philosopher1.requestForks();
            philosopher1.eat();
            philosopher1.releaseForks();
            philosopher2.requestForks();
            philosopher2.eat();
            philosopher2.releaseForks();
            Assertions.assertEquals((i + 1) * 6, philosopher1.getLamportClock().getTimestamp());
            Assertions.assertEquals(4 + (i * 6), philosopher2.getLamportClock().getTimestamp());
        }
    }

    @Test
    void serverIsPhilosopherLamportClockWorkingForNPhilosophers() {
        Philosopher philosopher4 = new Philosopher(1, "localhost", 49162, "localhost", 49161);
        Philosopher philosopher5 = new Philosopher(2, "localhost", 49160, "localhost", 49162);
        Philosopher philosopher6 = new Philosopher(3, "localhost", 49161, "localhost", 49160);

        // Get the Class object for the Philosopher class
        Class<?> philosopherClass = Philosopher.class;

        // Get the Field object for the eatInterval field
        Field eatIntervalField = null;
        try {
            eatIntervalField = philosopherClass.getDeclaredField("eatInterval");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        // Make the field accessible (since it's private)
        eatIntervalField.setAccessible(true);

        // Create an array to represent the new eatInterval value
        int[] newEatInterval = new int[]{100, 50};

        // Set the new value for eatInterval
        try {
            eatIntervalField.set(philosopher4, newEatInterval);
            eatIntervalField.set(philosopher5, newEatInterval);
            eatIntervalField.set(philosopher6, newEatInterval);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        Server server1 = new Server(philosopher4, 49160);
        Server server2 = new Server(philosopher5, 49161);
        Server server3 = new Server(philosopher6, 49162);
        try {
            // Wait for the server to finish
            server1.getServerLatch().await();
            server2.getServerLatch().await();
            server3.getServerLatch().await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Error while waiting for server to finish", e);
        }
        for (int i = 0; i < 2; i++) {
            philosopher4.requestForks();
            philosopher4.eat();
            philosopher4.releaseForks();
            philosopher5.requestForks();
            philosopher5.eat();
            philosopher5.releaseForks();
            philosopher6.requestForks();
            philosopher6.eat();
            philosopher6.releaseForks();
            Assertions.assertEquals((i + 1) * 6, philosopher4.getLamportClock().getTimestamp());
            Assertions.assertEquals((i + 1) * 6, philosopher5.getLamportClock().getTimestamp());
            Assertions.assertEquals(((i + 1) * 6) - 1, philosopher6.getLamportClock().getTimestamp());
        }
    }
}

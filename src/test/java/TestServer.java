import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertThrows;


public class TestServer {

    /**
     * Test if server exceptions are thrown when invalid philosophers are used
     */
    @Test
    void serverTestInvalidPhilosopherException() {
        assertThrows(IllegalArgumentException.class, () -> new Server(null, 49152));
    }

    /**
     * Test if server exceptions are thrown when invalid ports are used
     */

    @Test
    void serverTestInvalidPortException() {
        assertThrows(IllegalArgumentException.class, () -> new Server(new Philosopher(1, "localhost", 49170, "localhost", 49170), 49151));
        assertThrows(IllegalArgumentException.class, () -> new Server(new Philosopher(2, "localhost", 49171, "localhost", 49171), 65536));
    }

    /**
     * Test if client connections are accepted
     */
    @Test
    void serverTestAllNeighborsAreConnected() {
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
                return philosopher.getLeftNeighborSocket().isConnected() && philosopher.getRightNeighborSocket().isConnected();
            }
        } catch (Exception e) {
            throw new RuntimeException("Reflection failed", e);
        }
        return false;
    }

    /**
     * Test if Lamport clock is incremented correctly
     */
    @Test
    void serverTestPhilosopherLamportClockForTwoPhilosophers() {
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

    /**
     * Test if Lamport clock is incremented correctly
     */
    @Test
    void serverTestPhilosopherLamportClockForNPhilosophers() {
        Philosopher philosopher1 = new Philosopher(1, "localhost", 49162, "localhost", 49161);
        Philosopher philosopher2 = new Philosopher(2, "localhost", 49160, "localhost", 49162);
        Philosopher philosopher3 = new Philosopher(3, "localhost", 49161, "localhost", 49160);

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
            eatIntervalField.set(philosopher3, newEatInterval);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        Server server1 = new Server(philosopher1, 49160);
        Server server2 = new Server(philosopher2, 49161);
        Server server3 = new Server(philosopher3, 49162);
        try {
            // Wait for the server to finish
            server1.getServerLatch().await();
            server2.getServerLatch().await();
            server3.getServerLatch().await();
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
            philosopher3.requestForks();
            philosopher3.eat();
            philosopher3.releaseForks();
            Assertions.assertEquals((i + 1) * 6, philosopher1.getLamportClock().getTimestamp());
            Assertions.assertEquals((i + 1) * 6, philosopher2.getLamportClock().getTimestamp());
            Assertions.assertEquals(((i + 1) * 6) - 1, philosopher3.getLamportClock().getTimestamp());
        }
    }

    /**
     * Test if G-Counter is syncing across philosophers
     */
    @Test
    void serverTestPhilosopherGCounter() {
        Philosopher philosopher1 = new Philosopher(1, "localhost", 49165, "localhost", 49164);
        Philosopher philosopher2 = new Philosopher(2, "localhost", 49163, "localhost", 49165);
        Philosopher philosopher3 = new Philosopher(3, "localhost", 49164, "localhost", 49163);

        Server server1 = new Server(philosopher1, 49163);
        Server server2 = new Server(philosopher2, 49164);
        Server server3 = new Server(philosopher3, 49165);

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
            eatIntervalField.set(philosopher3, newEatInterval);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        try {
            // Wait for the server to finish
            server1.getServerLatch().await();
            server2.getServerLatch().await();
            server3.getServerLatch().await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Error while waiting for server to finish", e);
        }
        for (int j = 0; j < 10; j++) {
            philosopher1.eat();
            philosopher2.eat();
            philosopher2.eat();
        }
        philosopher1.sendCounter(philosopher1.getLeftNeighborSocket(), Direction.RIGHT, philosopher1.getLocalGCounter());
        philosopher2.sendCounter(philosopher2.getLeftNeighborSocket(), Direction.RIGHT, philosopher2.getLocalGCounter());
        while (philosopher1.getLocalGCounter().query() != 30) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        Assertions.assertEquals(30, philosopher1.getLocalGCounter().query());
    }

    /**
     * Test if philosophers have a reply after another philosopher has requested a fork
     */
    @Test
    void serverTestPhilosopherHasReply() {
        Philosopher philosopher1 = new Philosopher(1, "localhost", 49165, "localhost", 49164);
        Philosopher philosopher2 = new Philosopher(2, "localhost", 49163, "localhost", 49165);
        Philosopher philosopher3 = new Philosopher(3, "localhost", 49164, "localhost", 49163);

        Server server1 = new Server(philosopher1, 49163);
        Server server2 = new Server(philosopher2, 49164);
        Server server3 = new Server(philosopher3, 49165);
        try {
            // Wait for the server to finish
            server1.getServerLatch().await();
            server2.getServerLatch().await();
            server3.getServerLatch().await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Error while waiting for server to finish", e);
        }
        philosopher1.requestForks();
        Assertions.assertTrue(philosopher2.hasReply());
        Assertions.assertTrue(philosopher3.hasReply());
    }

    /**
     * Test if philosopher has both forks after requesting them
     */
    @Test
    void serverTestPhilosopherHasForks() {
        Philosopher philosopher1 = new Philosopher(1, "localhost", 49165, "localhost", 49164);
        Philosopher philosopher2 = new Philosopher(2, "localhost", 49163, "localhost", 49165);
        Philosopher philosopher3 = new Philosopher(3, "localhost", 49164, "localhost", 49163);

        Server server1 = new Server(philosopher1, 49163);
        Server server2 = new Server(philosopher2, 49164);
        Server server3 = new Server(philosopher3, 49165);
        try {
            // Wait for the server to finish
            server1.getServerLatch().await();
            server2.getServerLatch().await();
            server3.getServerLatch().await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Error while waiting for server to finish", e);
        }
        philosopher1.requestForks();
        philosopher1.releaseForks();
        philosopher2.requestForks();
        philosopher2.releaseForks();
        philosopher3.requestForks();
        philosopher3.releaseForks();
        philosopher1.requestForks();
        Assertions.assertTrue(philosopher1.hasLeftFork() && philosopher1.hasRightFork());
        Assertions.assertFalse(philosopher2.hasLeftFork() && philosopher2.hasRightFork());
        Assertions.assertFalse(philosopher3.hasLeftFork() && philosopher3.hasRightFork());
    }

    /**
     * Test if philosophers have received a ping from all neighbors
     */
    @Test
    void serverTestPhilosopherHasReceivedPing() {
        Philosopher philosopher1 = new Philosopher(1, "localhost", 49165, "localhost", 49164);
        Philosopher philosopher2 = new Philosopher(2, "localhost", 49163, "localhost", 49165);
        Philosopher philosopher3 = new Philosopher(3, "localhost", 49164, "localhost", 49163);

        Server server1 = new Server(philosopher1, 49163);
        Server server2 = new Server(philosopher2, 49164);
        Server server3 = new Server(philosopher3, 49165);
        try {
            // Wait for the server to finish
            server1.getServerLatch().await();
            server2.getServerLatch().await();
            server3.getServerLatch().await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Error while waiting for server to finish", e);
        }
        philosopher1.requestPing();
        philosopher2.requestPing();
        philosopher3.requestPing();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Assertions.assertTrue(philosopher1.isReceivedPingLeft() && philosopher1.isReceivedPingRight());
        Assertions.assertTrue(philosopher2.isReceivedPingLeft() && philosopher2.isReceivedPingRight());
        Assertions.assertTrue(philosopher3.isReceivedPingLeft() && philosopher3.isReceivedPingRight());
    }

    /**
     * Test if philosophers are requesting forks while another philosopher has the forks
     */
    @Test
    void serverPhilosopherIsRequesting() {
        Philosopher philosopher1 = new Philosopher(1, "localhost", 49165, "localhost", 49164);
        Philosopher philosopher2 = new Philosopher(2, "localhost", 49163, "localhost", 49165);
        Philosopher philosopher3 = new Philosopher(3, "localhost", 49164, "localhost", 49163);

        Server server1 = new Server(philosopher1, 49163);
        Server server2 = new Server(philosopher2, 49164);
        Server server3 = new Server(philosopher3, 49165);
        try {
            // Wait for the server to finish
            server1.getServerLatch().await();
            server2.getServerLatch().await();
            server3.getServerLatch().await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Error while waiting for server to finish", e);
        }
        philosopher2.requestForks();
        new Thread(philosopher3::requestForks).start();
        new Thread(philosopher1::requestForks).start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Assertions.assertTrue(philosopher3.isRequesting());
        Assertions.assertTrue(philosopher1.isRequesting());
    }

    /**
     * Test if philosopher is in critical section upon receiving replies from all neighbors
     */
    @Test
    void serverPhilosopherIsInCriticalSection() {
        Philosopher philosopher1 = new Philosopher(1, "localhost", 49165, "localhost", 49164);
        Philosopher philosopher2 = new Philosopher(2, "localhost", 49163, "localhost", 49165);
        Philosopher philosopher3 = new Philosopher(3, "localhost", 49164, "localhost", 49163);

        Server server1 = new Server(philosopher1, 49163);
        Server server2 = new Server(philosopher2, 49164);
        Server server3 = new Server(philosopher3, 49165);
        try {
            // Wait for the server to finish
            server1.getServerLatch().await();
            server2.getServerLatch().await();
            server3.getServerLatch().await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Error while waiting for server to finish", e);
        }
        philosopher1.requestForks();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Assertions.assertTrue(philosopher1.inCriticalSection());
    }

    /**
     * Test if philosopher is not requesting forks while in critical section
     */
    @Test
    void serverPhilosopherIsNotRequesting() {
        Philosopher philosopher1 = new Philosopher(1, "localhost", 49165, "localhost", 49164);
        Philosopher philosopher2 = new Philosopher(2, "localhost", 49163, "localhost", 49165);
        Philosopher philosopher3 = new Philosopher(3, "localhost", 49164, "localhost", 49163);

        Server server1 = new Server(philosopher1, 49163);
        Server server2 = new Server(philosopher2, 49164);
        Server server3 = new Server(philosopher3, 49165);
        try {
            // Wait for the server to finish
            server1.getServerLatch().await();
            server2.getServerLatch().await();
            server3.getServerLatch().await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Error while waiting for server to finish", e);
        }
        philosopher1.requestForks();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Assertions.assertFalse(philosopher1.isRequesting());
    }
}

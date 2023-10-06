import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestPhilosopher {

    /**
     * Test if philosopher exceptions are thrown when invalid ids are used
     */
    @Test
    void PhilosopherTestInvalidIdException() {
        assertThrows(IllegalArgumentException.class, () -> new Philosopher(-1, "localhost", 49152, "localhost", 49153));
        assertThrows(IllegalArgumentException.class, () -> new Philosopher(0, "localhost", 49152, "localhost", 49153));
        assertThrows(IllegalArgumentException.class, () -> new Philosopher(Integer.MIN_VALUE, "localhost", 49152, "localhost", 49153));
    }

    /**
     * Test if philosopher is created if valid ports are used
     */
    @Test
    void PhilosopherTestValidId() {
        assertDoesNotThrow(() -> {
            Philosopher philosopher1 = new Philosopher(1, "localhost", 49152, "localhost", 49152);
            new Server(philosopher1, 49152);
        });
        assertDoesNotThrow(() -> {
            Philosopher philosopher1 = new Philosopher(100, "localhost", 49153, "localhost", 49153);
            new Server(philosopher1, 49153);
        });
        assertDoesNotThrow(() -> {
            Philosopher philosopher1 = new Philosopher(Integer.MAX_VALUE, "localhost", 49154, "localhost", 49154);
            new Server(philosopher1, 49154);
        });
    }

    /**
     * Test if philosopher exceptions are thrown when invalid address are used
     */
    @Test
    void PhilosopherTestInvalidLeftNeighborAddressException() {
        assertThrows(IllegalArgumentException.class, () -> new Philosopher(1, null, 49152, "localhost", 49153));
    }

    /**
     * Test if philosopher exceptions are thrown when invalid address are used
     */
    @Test
    void PhilosopherTestInvalidRightNeighborAddressException() {
        assertThrows(IllegalArgumentException.class, () -> new Philosopher(1, "localhost", 49152, null, 49153));
    }

    /**
     * Test if philosopher exceptions are thrown when invalid address are used
     */
    @Test
    void PhilosopherTestInvalidAddressLeftAndRightNeighbor() {
        assertThrows(IllegalArgumentException.class, () -> new Philosopher(1, null, 49152, null, 49153));
    }

    /**
     * Test if philosopher is created when valid ports are used
     */
    @Test
    void PhilosopherTestValidPortLeftAndRightNeighbor() {
        assertDoesNotThrow(() -> {
            new Philosopher(1, "localhost", 49152, "localhost", 65535);
        });
    }

    /**
     * Test if philosopher exceptions are thrown when invalid ports are used
     */
    @Test
    void PhilosopherTestValidPortLeftInvalidPortRightNeighbor() {
        assertThrows(IllegalArgumentException.class, () -> new Philosopher(1, "localhost", 49152, "localhost", 65536));
    }

    /**
     * Test if philosopher exceptions are thrown when invalid ports are used
     */
    @Test
    void PhilosopherTestInvalidPortLeftValidPortRightNeighbor() {
        assertThrows(IllegalArgumentException.class, () -> new Philosopher(1, "localhost", 49151, "localhost", 65535));
    }

    /**
     * Test if philosopher exceptions are thrown when invalid ports are used
     */
    @Test
    void PhilosopherTestInvalidPortLeftAndRightNeighbor() {
        assertThrows(IllegalArgumentException.class, () -> new Philosopher(1, "localhost", 49151, "localhost", 65536));
    }
}

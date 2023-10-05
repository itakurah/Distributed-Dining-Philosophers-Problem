import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestPhilosopher {
    /**
     * Test if philosopher exceptions are thrown when invalid ids are used
     */
    @Test
    void PhilosopherInvalidIdException() {
        assertThrows(IllegalArgumentException.class, () -> new Philosopher(-1, "localhost", 49152, "localhost", 49153));
        assertThrows(IllegalArgumentException.class, () -> new Philosopher(0, "localhost", 49152, "localhost", 49153));
        assertThrows(IllegalArgumentException.class, () -> new Philosopher(Integer.MIN_VALUE, "localhost", 49152, "localhost", 49153));
    }

    /**
     * Test if philosopher is created if valid ports are used
     */
    @Test
    void PhilosopherValidId() {
        assertDoesNotThrow(() -> {
            new Philosopher(1, "localhost", 49152, "localhost", 49153);
        });
        assertDoesNotThrow(() -> {
            new Philosopher(100, "localhost", 49152, "localhost", 49153);
        });
        assertDoesNotThrow(() -> {
            new Philosopher(Integer.MAX_VALUE, "localhost", 49152, "localhost", 49153);
        });
    }

    /**
     * Test if philosopher exceptions are thrown when invalid address are used
     */
    @Test
    void PhilosopherInvalidLeftNeighborAddressException() {
        assertThrows(IllegalArgumentException.class, () -> new Philosopher(1, null, 49152, "localhost", 49153));
    }

    /**
     * Test if philosopher exceptions are thrown when invalid address are used
     */
    @Test
    void PhilosopherInvalidRightNeighborAddressException() {
        assertThrows(IllegalArgumentException.class, () -> new Philosopher(1, "localhost", 49152, null, 49153));
    }

    /**
     * Test if philosopher exceptions are thrown when invalid address are used
     */
    @Test
    void PhilosopherInvalidAddressLeftAndRightNeighbor() {
        assertThrows(IllegalArgumentException.class, () -> new Philosopher(1, null, 49152, null, 49153));
    }

    /**
     * Test if philosopher is created when valid ports are used
     */
    @Test
    void PhilosopherValidPortLeftAndRightNeighbor() {
        assertDoesNotThrow(() -> {
            new Philosopher(1, "localhost", 49152, "localhost", 65535);
        });
    }

    /**
     * Test if philosopher exceptions are thrown when invalid ports are used
     */
    @Test
    void PhilosopherValidPortLeftInvalidPortRightNeighbor() {
        assertThrows(InvalidPortException.class, () -> new Philosopher(1, "localhost", 49152, "localhost", 65536));
    }

    /**
     * Test if philosopher exceptions are thrown when invalid ports are used
     */
    @Test
    void PhilosopherInvalidPortLeftValidPortRightNeighbor() {
        assertThrows(InvalidPortException.class, () -> new Philosopher(1, "localhost", 49151, "localhost", 65535));
    }

    /**
     * Test if philosopher exceptions are thrown when invalid ports are used
     */
    @Test
    void PhilosopherInvalidPortLeftAndRightNeighbor() {
        assertThrows(InvalidPortException.class, () -> new Philosopher(1, "localhost", 49151, "localhost", 65536));
    }
}

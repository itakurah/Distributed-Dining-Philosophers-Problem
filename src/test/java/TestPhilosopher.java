import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

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
            Server server1 = new Server(philosopher1, 49152);
            try {
                // Wait for the server to finish
                server1.getServerLatch().await();
            } catch (InterruptedException e) {
                throw new RuntimeException("Error while waiting for server to finish", e);
            }
        });
        assertDoesNotThrow(() -> {
            Philosopher philosopher2 = new Philosopher(100, "localhost", 49153, "localhost", 49153);
            Server server1 = new Server(philosopher2, 49153);
            try {
                // Wait for the server to finish
                server1.getServerLatch().await();
            } catch (InterruptedException e) {
                throw new RuntimeException("Error while waiting for server to finish", e);
            }
        });
        assertDoesNotThrow(() -> {
            Philosopher philosopher3 = new Philosopher(Integer.MAX_VALUE, "localhost", 49154, "localhost", 49154);
            Server server1 = new Server(philosopher3, 49154);
            try {
                // Wait for the server to finish
                server1.getServerLatch().await();
            } catch (InterruptedException e) {
                throw new RuntimeException("Error while waiting for server to finish", e);
            }
        });
    }

    /**
     * Test if the given ipv4 addresses are valid
     */
    @Test
    void PhilosopherTestValidAddress() {
        Field field = null;
        try {
            field = Philosopher.class.getDeclaredField("isTest");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        field.setAccessible(true); // Make the field accessible
        try {
            field.setBoolean(null, true);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        String[] validIPAddresses = {
                "192.168.1.1",
                "10.0.0.1",
                "172.16.0.1",
                "127.0.0.1",
                "255.255.255.255"
        };
        for (String ipAddress : validIPAddresses) {
            assertDoesNotThrow(() -> new Philosopher(1,  ipAddress, 49152, ipAddress, 49153));
        }
        try {
            field.setBoolean(null, false);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Test if given ipv4 addresses are invalid
     */
    @Test
    void PhilosopherTestInvalidAddressException() {
        String[] invalidIPAddresses = {
                "localhosts",
                null,
                "2220.0.0.0",
                "123.123.3445.653",
                "999.999.999.999",
                "256.256.256.256",
                "111.abc.123.456",
                "1.2.3.4.5",
                "300.400.500.600",
                "255.255.255.256",
                "192.168.1.1a",
                "192.168.1.1#",
                "192.168.1.1@",
                "192.168.1.1$",
                "192.168.1.1%",
                "192.168.1.1^",
                "192.168.1.1&",
                "192.168.1.1*",
                "192.168.1.1(",
                "192.168.1.1)",
                "192.168.1.1<",
                "192.168.1.1>"
        };

        for (String ipAddress : invalidIPAddresses) {
            assertThrows(IllegalArgumentException.class, () -> new Philosopher(1, ipAddress, 49152, ipAddress, 49153));
        }
    }

    /**
     * Test if philosopher is created when valid ports are used
     */
    @Test
    void PhilosopherTestValidPortLeftAndRight() {
        assertDoesNotThrow(() -> {
            Philosopher philosopher1 = new Philosopher(1, "localhost", 49163, "localhost", 49163);
            Server server1 = new Server(philosopher1, 49163);
            try {
                // Wait for the server to finish
                server1.getServerLatch().await();
            } catch (InterruptedException e) {
                throw new RuntimeException("Error while waiting for server to finish", e);
            }
        });
    }

    /**
     * Test if philosopher exceptions are thrown when invalid ports are used
     */
    @Test
    void PhilosopherTestValidPortLeftInvalidPortRight() {
        Field field = null;
        try {
            field = Philosopher.class.getDeclaredField("isTest");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        field.setAccessible(true); // Make the field accessible
        try {
            field.setBoolean(null, true);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        assertThrows(IllegalArgumentException.class, () ->
                new Philosopher(1, "localhost", 49701, "localhost", 65536));
        try {
            field.setBoolean(null, false);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Test if philosopher exceptions are thrown when invalid ports are used
     */
    @Test
    void PhilosopherTestInvalidPortLeftValidPortRight() {
        Field field = null;
        try {
            field = Philosopher.class.getDeclaredField("isTest");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        field.setAccessible(true); // Make the field accessible
        try {
            field.setBoolean(null, true);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        assertThrows(IllegalArgumentException.class, () -> new Philosopher(1, "localhost", 49151, "localhost", 65535));
        try {
            field.setBoolean(null, false);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Test if philosopher exceptions are thrown when invalid ports are used
     */
    @Test
    void PhilosopherTestInvalidPortLeftAndRight() {
        assertThrows(IllegalArgumentException.class, () -> new Philosopher(1, "localhost", 49151, "localhost", 65536));
    }
}

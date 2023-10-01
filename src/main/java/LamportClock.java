import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lamport logical clock implementation
 */
class LamportClock implements Serializable {
    /**
     * The current Lamport timestamp
     */
    private final AtomicInteger timestamp;

    /**
     * Create a new Lamport clock
     */
    public LamportClock() {
        this.timestamp = new AtomicInteger(0);
    }

    /**
     * Get the current Lamport timestamp
     *
     * @return The current Lamport timestamp
     */
    public synchronized int getTimestamp() {
        return timestamp.get();
    }

    /**
     * Update the Lamport timestamp
     */
    public synchronized void update() {
        timestamp.incrementAndGet();
    }

    /**
     * Synchronize the Lamport timestamp with a received timestamp
     *
     * @param receivedTimestamp The received timestamp
     */
    public void synchronize(int receivedTimestamp) {
        int currentTimestamp = timestamp.get();
        int newTimestamp = Math.max(currentTimestamp, receivedTimestamp) + 1;
        timestamp.set(newTimestamp);
    }

    /**
     * Compare the Lamport timestamp with another timestamp
     *
     * @param otherTimestamp The other timestamp
     * @return -1 if the Lamport timestamp is smaller, 0 if they are equal, 1 if the Lamport timestamp is larger
     */
    public int compare(int otherTimestamp) {
        return Integer.compare(timestamp.get(), otherTimestamp);
    }
}

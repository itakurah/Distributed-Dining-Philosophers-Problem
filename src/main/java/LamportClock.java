import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

class LamportClock implements Serializable {
    private AtomicInteger timestamp;

    public LamportClock() {
        this.timestamp = new AtomicInteger(0);
    }

    // Get the current Lamport timestamp
    public int getTimestamp() {
        return timestamp.get();
    }

    // Update the Lamport timestamp with a new event
    public void update() {
        int newTimestamp = timestamp.incrementAndGet();
    }

    // Synchronize the Lamport timestamp with another process
    public void synchronize(int receivedTimestamp) {
        int currentTimestamp = timestamp.get();
        int newTimestamp = Math.max(currentTimestamp, receivedTimestamp) + 1;
        timestamp.set(newTimestamp);
    }

    // Compare Lamport timestamps
    public int compare(int otherTimestamp) {
        return Integer.compare(timestamp.get(), otherTimestamp);
    }

}

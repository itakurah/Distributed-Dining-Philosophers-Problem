import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A G-Counter is a grow-only counter that can only be incremented
 */
public class GCounter implements Serializable {
    /**
     * The id of the philosopher that the counter belongs to
     */
    private final int philosopherId;
    /**
     * The map of philosopher ids to counts
     */
    private final Map<Integer, Integer> counters;

    /**
     * Create a new G-Counter
     *
     * @param philosopherId The id of the philosopher that the counter belongs to
     */
    public GCounter(int philosopherId) {
        this.philosopherId = philosopherId;
        this.counters = new HashMap<>();
    }

    /**
     * Increment the counter
     */
    public synchronized void increment() {
        counters.put(philosopherId, counters.getOrDefault(philosopherId, 0) + 1);
    }

    /**
     * Query the counter
     *
     * @return The value of the counter
     */
    public synchronized int query() {
        int sum = 0;
        for (int count : counters.values()) {
            sum += count;
        }
        return sum;
    }

    /**
     * Merge this counter with another counter
     *
     * @param otherCounter The other counter
     */
    public synchronized void merge(GCounter otherCounter) {
        for (Map.Entry<Integer, Integer> entry : otherCounter.counters.entrySet()) {
            Integer philosopherId = entry.getKey();
            int count = entry.getValue();
            counters.put(philosopherId, Math.max(counters.getOrDefault(philosopherId, 0), count));
        }
    }
}
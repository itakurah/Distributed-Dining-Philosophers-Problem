import java.io.Serializable;

/**
 * A message that is sent between philosophers
 */
class Message implements Serializable {
    /**
     * The type of the message
     */
    private MessageType type;
    /**
     * The id of the philosopher that sent the message
     */
    private int philosopherId;
    /**
     * The timestamp of the message
     */
    private int timestamp;
    /**
     * The direction of the message
     */
    private Direction direction;

    /**
     * Create a new message
     *
     * @param type          The type of the message
     * @param philosopherId The id of the philosopher that sent the message
     * @param direction     The direction of the message
     * @param timestamp     The timestamp of the message
     */
    public Message(MessageType type, int philosopherId, Direction direction, int timestamp) {
        this.type = type;
        this.philosopherId = philosopherId;
        this.direction = direction;
        this.timestamp = timestamp;
    }

    /**
     * Create a new message
     *
     * @param type          The type of the message
     * @param philosopherId The id of the philosopher that sent the message
     * @param direction     The direction of the message
     */
    public Message(MessageType type, int philosopherId, Direction direction) {
        this.type = type;
        this.philosopherId = philosopherId;
        this.direction = direction;
    }

    /**
     * Get the type of the message
     * @return The type of the message
     */
    public MessageType getType() {
        return type;
    }
    /**
     * Get the id of the philosopher that sent the message
     * @return The id of the philosopher that sent the message
     */
    public int getPhilosopherId() {
        return philosopherId;
    }
    /**
     * Get the timestamp of the message
     * @return The timestamp of the message
     */
    public int getTimestamp() {
        return timestamp;
    }
    /**
     * Get the direction of the message
     * @return The direction of the message
     */
    public Direction getDirection() {
        return direction;
    }
}
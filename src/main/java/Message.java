import java.io.Serializable;

class Message implements Serializable {
    private MessageType type;
    private int philosopherId;
    private LamportClock lamportClock;
    private String direction;

    public Message(MessageType type, int philosopherId, String direction, LamportClock lamportClock) {
        this.type = type;
        this.philosopherId = philosopherId;
        this.direction = direction;
        this.lamportClock = lamportClock;
    }

    public MessageType getType() {
        return type;
    }

    public int getPhilosopherId() {
        return philosopherId;
    }

    public LamportClock getLamportClock() {
        return lamportClock;
    }

    public String getDirection() {
        return direction;
    }
}
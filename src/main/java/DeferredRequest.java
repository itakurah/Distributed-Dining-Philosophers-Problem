import java.net.Socket;

/**
 * A deferred request is a request that has been received but cannot be processed yet
 */
class DeferredRequest {
    /**
     * The socket of the client that sent the request
     */
    private final Socket socket;
    /**
     * The timestamp of the request
     */
    private final int requestTimestamp;

    /**
     * The direction of the request
     */
    private final Direction direction;

    /**
     * Create a new deferred request
     *
     * @param socket           The socket of the client that sent the request
     * @param requestTimestamp The timestamp of the request
     * @param direction        The direction of the request
     */
    public DeferredRequest(Socket socket, int requestTimestamp, Direction direction) {
        this.socket = socket;
        this.requestTimestamp = requestTimestamp;
        this.direction = direction;
    }

    /**
     * Get the socket of the client that sent the request
     *
     * @return The socket of the client that sent the request
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * Get the timestamp of the request
     *
     * @return The timestamp of the request
     */
    public int getRequestTimestamp() {
        return requestTimestamp;
    }

    /**
     * Get the direction of the request
     *
     * @return The direction of the request
     */
    public Direction getDirection() {
        return direction;
    }

    @Override
    public String toString() {
        return "DeferredRequest{" +
                "socket=" + socket +
                ", requestTimestamp=" + requestTimestamp +
                ", direction='" + direction + '\'' +
                '}';
    }
}
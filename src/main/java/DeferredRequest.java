import java.net.Socket;

/**
 * A deferred request is a request that has been received but cannot be processed yet
 */
public class DeferredRequest {
    /**
     * The socket of the client that sent the request
     */
    private final Socket socket;

    /**
     * The direction of the request
     */
    private final Direction direction;

    /**
     * Create a new deferred request
     *
     * @param socket    The socket of the client that sent the request
     * @param direction The direction of the request
     */
    public DeferredRequest(Socket socket, Direction direction) {
        this.socket = socket;
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
                ", direction='" + direction + '\'' +
                '}';
    }
}
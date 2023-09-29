import java.net.Socket;

class DeferredRequest {
    private Socket socket;
    private int requestTimestamp;

    private String direction;

    public DeferredRequest(Socket socket, int requestTimestamp, String direction) {
        this.socket = socket;
        this.requestTimestamp = requestTimestamp;
        this.direction = direction;
    }

    public Socket getSocket() {
        return socket;
    }

    public int getRequestTimestamp() {
        return requestTimestamp;
    }

    public String getDirection() {
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
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

/**
 * A client handler is responsible for handling requests from a client
 */
class ClientHandler implements Runnable {
    /**
     * The socket of the client
     */
    private final Socket clientSocket;
    /**
     * The philosopher that the client handler belongs to
     */
    private final Philosopher philosopher;

    /**
     * Create a new client handler
     *
     * @param clientSocket The socket of the client
     * @param philosopher  The philosopher that the client handler belongs to
     */
    public ClientHandler(Socket clientSocket, Philosopher philosopher) {
        this.clientSocket = clientSocket;
        this.philosopher = philosopher;
    }

    /**
     * Handle requests from the client
     */
    @Override
    public synchronized void run() {
        try {
            while (true) {
                try {
                    // Create an object input stream from the client socket
                    ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                    // Read a message from the client
                    Message receivedMessage = (Message) in.readObject();
                    // Handle the message
                    if (receivedMessage.getType() == MessageType.REQUEST) {
                        philosopher.receiveRequest(clientSocket, receivedMessage);
                    } else if (receivedMessage.getType() == MessageType.REPLY) {
                        philosopher.receiveReply(receivedMessage.getPhilosopherId(), receivedMessage.getDirection());
                    } else if (receivedMessage.getType() == MessageType.COUNTER) {
                        philosopher.receiveCounter(receivedMessage.getPhilosopherId(), receivedMessage.getDirection(), receivedMessage.getGCounter());
                    }
                } catch (EOFException e) {
                    // Client socket is closed, break out of the loop
                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

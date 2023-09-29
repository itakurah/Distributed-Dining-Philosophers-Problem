import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Philosopher philosopher;

    public ClientHandler(Socket clientSocket, Philosopher philosopher) {
        this.clientSocket = clientSocket;
        this.philosopher = philosopher;
    }

    @Override
    public synchronized void run() {
        try {
            // Setup input and output streams for communication

            while (true) {
                try {
                    ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                    // Read a message from the client
                    Message receivedMessage = (Message) in.readObject();

                    if (receivedMessage.getType() == MessageType.REQUEST) {
                        philosopher.receiveRequest(clientSocket, receivedMessage);
                        // Create a new thread to handle the request
//                        new Thread(() -> philosopher.receiveRequest(
//                                clientSocket,
//                                receivedMessage.getTimestamp(),
//                                receivedMessage.getPhilosopherId(),
//                                receivedMessage.getDirection()
//                        )).start();
                    } else if (receivedMessage.getType() == MessageType.REPLY) {
                        philosopher.receiveReply(receivedMessage.getPhilosopherId(), receivedMessage.getDirection());
//                        new Thread(() -> philosopher.receiveReply(
//                                receivedMessage.getPhilosopherId(),
//                                receivedMessage.getDirection()
//                        )).start();
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

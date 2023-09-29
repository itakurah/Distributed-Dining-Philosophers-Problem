import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiningPhilosophers1 {
    public static void main(String[] args) throws IOException {
        //InetSocketAddress leftNeighborAdress = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
        //InetSocketAddress rightNeighborAdress = new InetSocketAddress(args[4], Integer.parseInt(args[5]));
        InetSocketAddress leftNeighborAdress = new InetSocketAddress("localhost", 5003);
        InetSocketAddress rightNeighborAdress = new InetSocketAddress("localhost", 5002);

//        Philosopher philosopher = new Philosopher(Integer.parseInt(args[0]));
        Philosopher philosopher = new Philosopher(1);
        // Connect to left and right neighbors

//        ServerSocket serverSocket = new ServerSocket(Integer.parseInt(args[1])); // Use a fixed port for listening
        //ServerSocket serverSocket = new ServerSocket(5001); // Use a fixed port for listening

        final int PORT = 5001;
        final int MAX_THREADS = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREADS);

        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("Server started on port " + PORT);
                while (true) {
                    try {
                        Socket socket = serverSocket.accept();
                        System.out.println("Client connected: " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());

                        // Delegate client handling to a thread from the thread pool
                        executorService.execute(new ClientHandler(socket, philosopher));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();

        philosopher.connectToLeftNeighbor(leftNeighborAdress);
        philosopher.connectToRightNeighbor(rightNeighborAdress);

        new Thread(() -> {
            while (true) {
                philosopher.think();
                philosopher.requestForks();
                philosopher.eat();
                philosopher.releaseForks();
            }
        }).start();

    }
}

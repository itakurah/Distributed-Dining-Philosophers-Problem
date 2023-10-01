import java.net.InetSocketAddress;
/**
 * Main class to be run with docker
 */
public class Application {
    public static void main(String[] args) {
        InetSocketAddress leftNeighborAddress = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
        InetSocketAddress rightNeighborAddress = new InetSocketAddress(args[4], Integer.parseInt(args[5]));
        Philosopher philosopher = new Philosopher(Integer.parseInt(args[0]));
        Server server = new Server(philosopher, Integer.parseInt(args[1]));
        server.start();
        // Connect to left and right neighbors
        philosopher.connectToLeftNeighbor(leftNeighborAddress);
        philosopher.connectToRightNeighbor(rightNeighborAddress);

        try {
            // Wait for the server to finish
            server.getServerLatch().await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            while (true) {
                philosopher.think();
                philosopher.requestForks();
                philosopher.eat();
                philosopher.releaseForks();
            }
        }).start();

        new Thread(() -> {
            while (true) {
                philosopher.updateNeighborCounter();
            }
        }).start();
    }
}

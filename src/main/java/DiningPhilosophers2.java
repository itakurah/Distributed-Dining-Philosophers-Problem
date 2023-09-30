import java.net.InetSocketAddress;

public class DiningPhilosophers2 {
    public static void main(String[] args) {
//        InetSocketAddress leftNeighborAdress = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
//        InetSocketAddress rightNeighborAdress = new InetSocketAddress(args[4], Integer.parseInt(args[5]));
        InetSocketAddress leftNeighborAdress = new InetSocketAddress("localhost", 5001);
        InetSocketAddress rightNeighborAdress = new InetSocketAddress("localhost", 5003);

//        Philosopher philosopher = new Philosopher(Integer.parseInt(args[0]));
        Philosopher philosopher = new Philosopher(2);
        Server server = new Server(philosopher, 5002);
        server.start();
        // Connect to left and right neighbors
        philosopher.connectToLeftNeighbor(leftNeighborAdress);
        philosopher.connectToRightNeighbor(rightNeighborAdress);

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
    }
}

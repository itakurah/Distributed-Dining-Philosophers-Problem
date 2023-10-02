/**
 * A game is responsible for running the game loop
 */
public class Game {
    /**
     * The philosopher that the game belongs to
     */
    private final Philosopher philosopher;

    /**
     * Create a new game
     *
     * @param philosopher The philosopher that the game belongs to
     */
    public Game(Philosopher philosopher) {
        this.philosopher = philosopher;
    }

    /**
     * Run the game loop
     */
    public void start() {
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

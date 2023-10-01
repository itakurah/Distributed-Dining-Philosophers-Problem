import java.io.FileWriter;
import java.io.IOException;

public class GenerateCompose {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java GenerateCompose <number_of_instances> <server_port>");
            System.exit(1);
        }

        int numInstances = Integer.parseInt(args[0]);
        int serverPort = Integer.parseInt(args[1]);
        boolean isFirst = true;
        boolean isLast = false;

        try (FileWriter writer = new FileWriter("docker-compose.yml")) {
            writer.write("version: '3'\n");
            writer.write("services:\n");

            for (int i = 1; i <= numInstances; i++) {
                int leftNeighborPort = serverPort + i;
                int rightNeighborPort = serverPort + i + 2;
                if(i == numInstances) {
                    isLast = true;
                }
                writer.write("  app" + i + ":\n");
                writer.write("    image: ddpp:latest\n");
                writer.write("    command: ['java', '-jar', '/usr/app/vs-1.0-SNAPSHOT.jar', '" + i + "', '" + leftNeighborPort + "', '" + (isFirst ? "app" + numInstances : "app" + (i-1)) + "', '" + (isFirst ? leftNeighborPort + Integer.parseInt(args[0]) - 1 : leftNeighborPort - 1) + "', '" + (isLast ? "app" + (numInstances-i+1) : "app" + (i+1)) + "', '" + (isLast ? serverPort + 1 : leftNeighborPort + 1) + "']\n");
                writer.write("    networks:\n");
                writer.write("      - network\n");
                isFirst = false;

            }
            writer.write("networks:\n");
            writer.write("  network:\n");
            writer.write("    driver: bridge");

            System.out.println("docker-compose.yml file with " + numInstances + " instances of app2 has been generated.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

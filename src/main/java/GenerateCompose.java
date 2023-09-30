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
                writer.write("    command: ['java', '-jar', '/app/Application.jar', '" + i + "', '" + leftNeighborPort + "', 'localhost', '" + ((isFirst)?leftNeighborPort+Integer.parseInt(args[0])-1:leftNeighborPort-1) + "', 'localhost', '"+((isLast)?serverPort+1:leftNeighborPort+1)+"']\n");
                writer.write("    ports:\n");
                writer.write("      - '" + leftNeighborPort + ":5000'\n");
                isFirst = false;

            }

            System.out.println("docker-compose.yml file with " + numInstances + " instances of app2 has been generated.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package server;

import java.io.IOException;

public class ServerMain extends Thread {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("USAGE: server PORT");
            System.exit(1);
        }

        try (Server server = new Server(Integer.parseInt(args[0]))) {
            server.listen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package client;

public class ClientMain {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("usage: FilePath ip port");
            System.exit(0);
        }
        Client client = new Client(args[0], args[1], Integer.parseInt(args[2]));
        client.sendFile();
    }
}

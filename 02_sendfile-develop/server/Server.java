package server;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends Thread implements AutoCloseable, Closeable {

    private ServerSocket mainSocket;
    private static final boolean SERVER_IS_ONLINE = true;
    private int clientCounter;

    Server(int port) {
        try {
            mainSocket = new ServerSocket(port, 5000);
            new File("uploads").mkdirs();
            clientCounter = 0;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void listen() {
        while (!isInterrupted()) {
            Socket socket;
            try {
                socket = mainSocket.accept();
                Thread thread = new Thread(new ServerReadThread(socket), "Client " + clientCounter);
                clientCounter++;
                thread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public void close() throws IOException {
        mainSocket.close();
    }
}

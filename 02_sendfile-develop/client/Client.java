package client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;

public class Client {
    private static final double MAX_FILE_SIZE = 1e12; //1TB
    private static final int MAX_FILENAME_SIZE = 4096;

    private Socket mySocket;
    private File file;
    private InputStream inputStream;



    private void sendToSocket(byte[] data) {
        try {
            mySocket.getOutputStream().write(data);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }

    private void sendToSocket(byte[] data, int desiredLength) {
        try {
            if (desiredLength == data.length) {
                mySocket.getOutputStream().write(data);
            }
            else if (desiredLength > data.length) {
                byte[] newData = new byte[desiredLength];
                System.arraycopy(data, 0, newData, desiredLength - data.length, data.length);
                mySocket.getOutputStream().write(newData);
            } else {
                byte[] newData = Arrays.copyOf(data, desiredLength);
                mySocket.getOutputStream().write(newData);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public Client(String path, String ip, Integer port) {
        try {
            file = new File(path);
            inputStream = new FileInputStream(file);
            mySocket = new Socket(ip, port);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw new ExceptionInInitializerError("Can`t connect!");
        }
    }

    public void sendFile() {
        checkFile();
        byte[] fileName = file.getName().getBytes(Charset.forName("UTF-8"));
        System.out.println("Sending name size...");
        sendToSocket(BigInteger.valueOf(fileName.length).toByteArray(), 2);

        System.out.println("Sending name...");
        sendToSocket(fileName);

        System.out.println("Sending file size....");
        sendToSocket(BigInteger.valueOf(file.length()).toByteArray(), 8);

        byte[] buffer = new byte[4096];
        System.out.println("Sending file...");
        int readBytes;
        try {
            while ((readBytes = inputStream.read(buffer)) > 0) {
                sendToSocket(buffer, readBytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                mySocket.close();
                System.out.println("Error - connection lost!");
                System.exit(0);
            } catch (IOException e1) {}
        }


        try {
            byte success = ((byte) mySocket.getInputStream().read());
            if (success == 1) {
                System.out.println("File uploaded successfully!");
                mySocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void checkFile() {
        if (file.length() > MAX_FILE_SIZE) {
            System.out.println("Max file size " + MAX_FILE_SIZE + " (in bytes)");
            System.exit(1);
        }
        if (file.getName().length() > MAX_FILENAME_SIZE) {
            System.out.println("File name length must not exceed " + MAX_FILENAME_SIZE+ " bytes");
            System.exit(1);
        }
    }


}

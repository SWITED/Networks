package server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class ServerReadThread implements Runnable {

    private Socket socket;
    private InputStream inputStream;

    ServerReadThread(Socket socket) {
        try {
            this.socket = socket;
            this.inputStream = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + ": starting download...");
        Timer timer = new Timer();
        try {
            byte[] filenameSize = new byte[2];
            inputStream.read(filenameSize, 0, 2);

            byte[] filename = new byte[new BigInteger(filenameSize).intValue()];
            inputStream.readNBytes(filename, 0, filename.length);

            byte[] fileLength = new byte[8];
            inputStream.read(fileLength, 0, 8);

            long bytesLeft = new BigInteger(fileLength).longValue();
            byte[] buffer = new byte[4096];
            File file = new File("uploads/" + new String(filename).trim());
            file.createNewFile();
            int bytesRead;
            final ArrayList<Integer> bytesReadForTimer = new ArrayList<>();
            bytesReadForTimer.add(0);
            timer.schedule(new SpeedTesterTask(bytesReadForTimer), 0, 3000);
            do {
                bytesRead = inputStream.read(buffer, 0, bytesLeft < 4096L ? Math.toIntExact(bytesLeft) : 4096);
                bytesLeft = bytesLeft - bytesRead;
                bytesReadForTimer.set(0, bytesReadForTimer.get(0) + bytesRead);

            } while (bytesLeft > 0);
            long fileSize = ByteBuffer.wrap(fileLength).getLong();
            if (file.length() == fileSize) {
                System.out.println("File downloaded.");
            } else {
                System.out.println("Error - file corrupted!");
            }

            timer.cancel();
            socket.getOutputStream().write(1);
            socket.getOutputStream().flush();
            socket.close();
        } catch (IOException e) {
            System.out.println(Thread.currentThread().getName() + " got an error! Closing connection.");
            try {
                timer.cancel();
                socket.close();
            } catch (IOException e1) {}
        }
    }
}

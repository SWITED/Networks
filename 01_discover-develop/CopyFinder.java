package com.company;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static java.lang.Math.abs;


public class CopyFinder
{
    private MulticastSocket multicastSocket;        //client that will listen Constants.PORT
    private DatagramSocket datagramSocket;
    private InetAddress ipOfCopy;
    private HashMap<SocketAddress, Long> copyesMap; //Map'a в которой будут хранится наши сокеты
    private DatagramPacket receivePacket;
    private DatagramPacket sendPacket;
    private long nextMulticastTime = 0;


    public CopyFinder(String address, int portOfCopy)
    {
        try {
            copyesMap = new HashMap<>();
            ipOfCopy = InetAddress.getByName(address);
            multicastSocket = new MulticastSocket(Constants.PORT);
            multicastSocket.joinGroup(ipOfCopy);
            datagramSocket = new DatagramSocket(portOfCopy);
            receivePacket = new DatagramPacket(new byte[Constants.MESSAGE.length()], Constants.MESSAGE.length());
            sendPacket = new DatagramPacket(Constants.MESSAGE.getBytes(Constants.CHARSET),  Constants.MESSAGE.length(), ipOfCopy, Constants.PORT);
        } catch (Exception ex) {
            System.err.println(ex.getLocalizedMessage());
            System.exit(1);
        }
    }

    private void packetSend() throws IOException {
        int countCopies = copyesMap.size();

        if(nextMulticastTime < System.currentTimeMillis()) {
            nextMulticastTime = System.currentTimeMillis() + Constants.MULTICAST_DELAY;
            datagramSocket.send(sendPacket);
        }

        copyesMap.entrySet().removeIf(socketAddressLongEntry -> {
            if (abs(System.currentTimeMillis() - socketAddressLongEntry.getValue()) > Constants.LIFE_TIMEOUT) {
                return true;
            }
            return false;
        });

        if(countCopies > copyesMap.size()) {
            printCopyes();
        }

        int timeout = (int) (nextMulticastTime - System.currentTimeMillis());
        if (timeout <= 0) {
            timeout = 1;
        }
        multicastSocket.setSoTimeout(timeout);
    }


    private void packetRecieve() throws IOException {
        multicastSocket.receive(receivePacket);
        String message = new String(receivePacket.getData(), Constants.CHARSET);
        if(message.equals(Constants.MESSAGE)) {
            if(copyesMap.put(receivePacket.getSocketAddress(), System.currentTimeMillis() + Constants.LIFE_TIMEOUT) == null) {
                printCopyes();
            }
        }
    }


    public void start()
    {
        while (true)
        {
            try{
                packetSend();
                packetRecieve();
            } catch (IOException err) {
                System.out.println(err.getLocalizedMessage());
            }
        }
    }


    private void printCopyes() {
        int index = 0;
        System.out.println("_______________________________");
        System.out.println("Number of Copyes : " + (copyesMap.size() - 1));
        for (SocketAddress key : copyesMap.keySet()) {
            if(index == 0) {
                System.out.println("I am /IP:PORT = "+ key);
            } else {
                System.out.println("My Copy, /IP:PORT = "+ key);
            }
            ++index;
        }
        System.out.println("_______________________________");
    }
}

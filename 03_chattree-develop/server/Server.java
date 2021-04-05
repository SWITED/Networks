package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;

public class Server {
    private static final int CONFIRMATION_PERIOD = 3000;
    private static final int SUBSTITUTION_PERIOD = 15000;
    private static final int MAX_TIME_SENT = 3;

    private Random random = new Random();

    private String name;
    private Integer lossPercentage;
    private Node parent;
    private Node substitutor;

    private DatagramSocket datagramSocket;

    private List<Node> children;
    private List<DatagramWrapper> unconfirmedMessages;

    public Server(String name, Integer lossPercentage, Integer selfPort) throws SocketException {
        this.name = name;
        this.lossPercentage = lossPercentage;

        unconfirmedMessages = new Vector<>();
        datagramSocket = new DatagramSocket(selfPort);
        children = new Vector<>();
    }

    public Server(String name, Integer lossPercentage, Integer selfPort, String parentIp, Integer parentPort)
            throws UnknownHostException, SocketException {
        this(name, lossPercentage, selfPort);
        this.parent = new Node(InetAddress.getByName(parentIp), parentPort);
    }

    public void start() throws IOException, ClassNotFoundException {
        System.out.println("Node \"" + name + "\" is working now.");
        //Создаем и запускаем поток, который будет считывать с клавиатуры сообщения и отправлять их
        //дочерним узлам и родительскому узлу
        Thread consoleReaderThread = new Thread(new MessageSender());
        consoleReaderThread.start();

        Timer confirmationTimer = new Timer();
        confirmationTimer.schedule(new ResendConfirmationTimerTask(unconfirmedMessages), 0, CONFIRMATION_PERIOD);

        Timer substitutionTimer = new Timer();
        substitutionTimer.schedule(new SubstitutionTimerTask(), 0, SUBSTITUTION_PERIOD);

        //Если на вход программе были передана информация о родительском узле, то на необходимо
        //уведомить этого родителя о том, что данный узел является его дочерним узлом
        if (!isRootNode()) {
            Message helloMessage = new Message(UUID.randomUUID(), "", name, Message.MessageType.REGISTER);
            DatagramWrapper datagramWrapper = new DatagramWrapper(helloMessage, parent);
            unconfirmedMessages.add(datagramWrapper);
            sendMessage(datagramWrapper);
        }

        //Принимаем пакеты
        while (true) {
            DatagramPacket receivedPacket = new DatagramPacket(new byte[2048], 0, 2048);
            datagramSocket.receive(receivedPacket);
            //Имитируем сетевую потерю пакета
            if (random.nextInt(100) < lossPercentage) {
                System.out.println("Some packet was lost");
                continue;
            }

            //Формируем обертку над пакетом, которая позволит нам десерилизовать данные в пакете
            DatagramWrapper datagramWrapper = new DatagramWrapper(receivedPacket);
            System.out.println("Received message: " + datagramWrapper.getMessage());

            //Обрабатываем данные в пакете в зависимости от типа сообщения
            switch (datagramWrapper.getMessage().getType()) {
                case REGISTER:
                    Node node = new Node(receivedPacket.getAddress(), receivedPacket.getPort());
                    //Если данный узел не явялется дочерним, до добавляем его в список дочерних
                    if (children.indexOf(node) == -1) {
                        children.add(node);
                    }
                    //Отправляем пакет подтверждения
                    sendConfirmation(datagramWrapper);
                    break;
                case TEXT:
                    if (hasSuchUuid(datagramWrapper.getMessage().getUuid()) == -1) {
                        System.out.println(datagramWrapper.getMessage().getSenderName() + ": " + datagramWrapper.getMessage().getText());

                        //Если есть родитель и отправитель не родитель, то отправляем сообщение родителю
                        if (!isRootNode() && !(parent.getAddress().equals(datagramWrapper.getNode().getAddress())
                                && parent.getPort().equals(datagramWrapper.getNode().getPort()))) {
                            //Пробрасываем сообщение дальше
                            forwardMessage(datagramWrapper.getMessage(), parent);
                        }

                        //Отправляем сообщение дочерним узлам
                        for (Node child : children) {
                            //Если отправитель не дочерний узел, отправляем ему
                            if (!(child.getAddress().equals(datagramWrapper.getNode().getAddress()) &&
                                    child.getPort().equals(datagramWrapper.getNode().getPort()))) {
                                //Пробрасываем сообщение дальше
                                forwardMessage(datagramWrapper.getMessage(), child);
                            }
                        }
                    }
                    sendConfirmation(datagramWrapper);

                    break;
                case CONFIRMATION:
                    confirmMessageWithUuid(datagramWrapper.getMessage().getUuid());
                    break;
                case SUBSTITUTION:
                    substitutor = datagramWrapper.getMessage().getSubstitutor();
                    break;
            }
        }
    }

    //Отправляем пакет с сообщением, которое имеет тип "подтверждение" и UUID принятого сообщения
    private void sendConfirmation(DatagramWrapper datagramWrapper) throws IOException {
        DatagramWrapper wrapper = new DatagramWrapper(new Message(datagramWrapper.getMessage()), datagramWrapper.getNode());
        wrapper.getMessage().setType(Message.MessageType.CONFIRMATION);
        sendMessage(wrapper);
    }

    //Если UUID совподает с один из UUID из списка неподтвержденных сообщений, удаляем его из списка, тем самым
    //подтверждая отправку сообщения
    private synchronized void confirmMessageWithUuid(UUID uuid) {
        for (int i = 0; i < unconfirmedMessages.size(); i++) {
            if (unconfirmedMessages.get(i).getMessage().getUuid().equals(uuid)) {
                unconfirmedMessages.remove(i);
                return;
            }
        }
    }

    //Проверяем совпадения UUID с одним из UUID сообщения из списка неподтвержденных сообщений
    private synchronized int hasSuchUuid(UUID uuid) {
        for (int i = 0; i < unconfirmedMessages.size(); i++) {
            if (unconfirmedMessages.get(i).getMessage().getUuid().equals(uuid)) {
                return i;
            }
        }
        return -1;
    }

    private void forwardMessage(Message message, Node node) throws IOException {
        Message msg = new Message(message);
        //msg.setUuid(UUID.randomUUID());
        DatagramWrapper datagramWrapper = new DatagramWrapper(msg, node);
        unconfirmedMessages.add(datagramWrapper);
        sendMessage(datagramWrapper);
    }

    private class MessageSender implements Runnable {
        @Override
        public void run() {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in))) {
                while (true) {
                    String text = bufferedReader.readLine();
                    sendMessageToEveryone(new Message(UUID.randomUUID(), text, name, Message.MessageType.TEXT));
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private synchronized void sendMessageToEveryone(Message message) throws IOException {
        for (Node child : children) {
            DatagramWrapper datagramWrapper = new DatagramWrapper(message, child);
            unconfirmedMessages.add(datagramWrapper);
            sendMessage(datagramWrapper);
        }

        if (!isRootNode()) {
            DatagramWrapper datagramWrapper = new DatagramWrapper(message, parent);
            unconfirmedMessages.add(datagramWrapper);
            sendMessage(datagramWrapper);
        }
    }

    private synchronized void sendMessage(DatagramWrapper datagramWrapper) throws IOException {
        System.out.println("Sending message: " + datagramWrapper.getMessage());
        datagramSocket.send(datagramWrapper.convertToDatagramPacket());
    }

    private boolean isRootNode() {
        return (parent == null);
    }

    private class ResendConfirmationTimerTask extends TimerTask {
        private List<DatagramWrapper> unconfirmedMessages;

        ResendConfirmationTimerTask(List<DatagramWrapper> unconfirmedMessages) {
            this.unconfirmedMessages = unconfirmedMessages;
        }

        @Override
        public void run() {
            for (int i = 0; i < unconfirmedMessages.size(); ++i) {
                DatagramWrapper datagramWrapper = unconfirmedMessages.get(i);
                if (datagramWrapper.getTimesSent() >= MAX_TIME_SENT) {
                    try {
                        deleteNodeAsInWrapper(datagramWrapper);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    unconfirmedMessages.remove(datagramWrapper);
                    break;
                }
                datagramWrapper.increaseTimesSent();
                try {
                    sendMessage(datagramWrapper);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private synchronized void deleteNodeAsInWrapper(DatagramWrapper datagramWrapper) throws IOException {
        for (int i = 0; i < children.size(); ++i) {
            if (children.get(i).equals(datagramWrapper.getNode())) {
                System.out.println("Child " + children.get(i).getAddress() + ": " + children.get(i).getAddress() + " deleted");
                children.remove(i);
                return;
            }
        }
        if (parent.equals(datagramWrapper.getNode())) {
            System.out.println("Parent " + parent.getAddress() + ": " + parent.getPort() + " deleted");
            parent = substitutor;
            substitutor = null;

            if (parent != null) {
                DatagramWrapper helloYouAreMyFather = new DatagramWrapper(new Message(UUID.randomUUID(), "",
                        name, Message.MessageType.REGISTER), parent);
                sendMessage(helloYouAreMyFather );
            }
        }
    }

    private class SubstitutionTimerTask extends TimerTask {
        @Override
        public void run() {
            for (Node child : children) {
                try {
                    DatagramWrapper datagramWrapper;
                    if (parent != null) {
                        datagramWrapper = new DatagramWrapper(new Message(UUID.randomUUID(), "", name,
                                Message.MessageType.SUBSTITUTION, parent), child);
                    } else {
                        if (child.equals(children.get(0))) {
                            datagramWrapper = new DatagramWrapper(new Message(UUID.randomUUID(), "", name,
                                    Message.MessageType.SUBSTITUTION, null), child);
                        } else {
                            datagramWrapper = new DatagramWrapper(new Message(UUID.randomUUID(), "", name,
                                    Message.MessageType.SUBSTITUTION, children.get(0)), child);
                        }
                    }
                    sendMessage(datagramWrapper);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

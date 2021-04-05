package server;

import java.io.Serializable;
import java.util.UUID;

public class Message implements Serializable {
    private UUID uuid;
    private String text;
    private String senderName;
    private MessageType type;
    private Node substitutor;

    public enum MessageType {
        REGISTER,
        TEXT,
        CONFIRMATION,
        SUBSTITUTION
    }

    public Message(UUID uuid, String message, String senderName, MessageType messageType) {
        this.uuid = uuid;
        this.text = message;
        this.senderName = senderName;
        this.type = messageType;
    }

    public Message(UUID uuid, String message, String senderName, MessageType messageType, Node substitutor) {
        this(uuid, message, senderName, messageType);
        this.substitutor = substitutor;
    }

    public Message(Message message) {
        this.uuid = message.uuid;
        this.text = message.text;
        this.senderName = message.senderName;
        this.type = message.type;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getText() {
        return text;
    }

    public String getSenderName() {
        return senderName;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Node getSubstitutor() {
        return substitutor;
    }

    @Override
    public String toString() {
        return "UUID: " + uuid + ". " + "Message: " + text + ". "
                + "Sender name: " + senderName + ". " + "Message type: " + type + ". ";
    }
}

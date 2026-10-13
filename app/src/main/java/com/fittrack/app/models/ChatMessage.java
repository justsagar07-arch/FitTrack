package com.fittrack.app.models;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_AI = 1;

    private String text;
    private int senderType;
    private long timestamp;

    public ChatMessage(String text, int senderType, long timestamp) {
        this.text = text;
        this.senderType = senderType;
        this.timestamp = timestamp;
    }

    public String getText() {
        return text;
    }

    public int getSenderType() {
        return senderType;
    }

    public long getTimestamp() {
        return timestamp;
    }
}

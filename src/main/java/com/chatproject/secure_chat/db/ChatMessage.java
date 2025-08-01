package com.chatproject.secure_chat.db;

public class ChatMessage {
    private String sender;
    private String receiver;
    private String message;
    private String timestamp;


    //생성자
    public ChatMessage(String sender, String receiver, String message, String timestamp){
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.timestamp = timestamp;
    }

    // getter 메서드들
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public String getMessage() { return message; }
    public String getTimestamp() { return timestamp; }
}

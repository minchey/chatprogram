package com.chatproject.secure_chat.db;

public class ChatMessage {
    private String sender;
    private String receiver;
    private String ciphertext;           // ★ 평문 대신 AES 암호문(Base64)
    private String aesKeyForReceiver;    // ★ 수신자 공개키로 암호화된 AES 키(Base64)
    private String aesKeyForSender;      // ★ 발신자 공개키로 암호화된 AES 키(Base64)
    private String timestamp;

    public ChatMessage(String sender, String receiver,
                       String ciphertext, String aesKeyForReceiver,
                       String aesKeyForSender, String timestamp) {
        this.sender = sender;
        this.receiver = receiver;
        this.ciphertext = ciphertext;
        this.aesKeyForReceiver = aesKeyForReceiver;
        this.aesKeyForSender = aesKeyForSender;
        this.timestamp = timestamp;
    }

    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public String getCiphertext() { return ciphertext; }
    public String getAesKeyForReceiver() { return aesKeyForReceiver; }
    public String getAesKeyForSender() { return aesKeyForSender; }
    public String getTimestamp() { return timestamp; }
}

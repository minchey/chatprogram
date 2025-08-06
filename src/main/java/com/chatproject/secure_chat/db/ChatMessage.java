package com.chatproject.secure_chat.db;

/**
 * DB에 저장되는 메시지 1건을 표현하는 모델.
 * - 평문은 절대 저장하지 않음.
 * - ciphertext: AES로 암호화된 본문(Base64)
 * - aesKeyForReceiver: 수신자 공개키로 RSA 암호화된 AES 키(Base64)
 * - aesKeyForSender : 발신자 공개키로 RSA 암호화된 AES 키(Base64)
 * - timestamp: ISO 문자열 등(정렬/조회용)
 */
public class ChatMessage {
    private String sender;
    private String receiver;
    private String ciphertext;
    private String aesKeyForReceiver;
    private String aesKeyForSender;
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

    // Getters
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public String getCiphertext() { return ciphertext; }
    public String getAesKeyForReceiver() { return aesKeyForReceiver; }
    public String getAesKeyForSender() { return aesKeyForSender; }
    public String getTimestamp() { return timestamp; }
}

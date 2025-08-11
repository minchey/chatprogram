package com.chatproject.secure_chat.db;

/**
 * ChatMessage
 *
 * 📌 DB(messages 테이블)에 저장되는 "메시지 한 건"을 표현하는 데이터 모델(VO, DTO 성격).
 *  - 평문(Plain Text)은 절대 저장하지 않음 → 항상 AES 암호문 형태로 저장.
 *  - 각 AES 키는 RSA로 암호화되어 발신자/수신자 전용으로 따로 저장.
 *
 * 필드 설명:
 *  sender              → 메시지를 보낸 사용자의 닉네임
 *  receiver            → 메시지를 받은 사용자의 닉네임
 *  ciphertext          → AES로 암호화한 본문(Base64 인코딩 문자열)
 *  aesKeyForReceiver   → 수신자 공개키로 RSA 암호화된 AES 키(Base64)
 *  aesKeyForSender     → 발신자 공개키로 RSA 암호화된 AES 키(Base64) — 없을 수도 있음
 *  timestamp           → 메시지 전송 시각(문자열, ISO 포맷 등)
 */
public class ChatMessage {
    /** 보낸 사람 닉네임 */
    private String sender;
    /** 받는 사람 닉네임 */
    private String receiver;
    /** AES 암호문(Base64) */
    private String ciphertext;
    /** 수신자 공개키로 RSA 암호화된 AES 키(Base64) */
    private String aesKeyForReceiver;
    /** 발신자 공개키로 RSA 암호화된 AES 키(Base64) */
    private String aesKeyForSender;
    /** 메시지 타임스탬프 */
    private String timestamp;

    /**
     * 모든 필드를 초기화하는 생성자
     * @param sender 보낸 사람
     * @param receiver 받는 사람
     * @param ciphertext AES 암호문(Base64)
     * @param aesKeyForReceiver 수신자용 AES 키(RSA 암호문)
     * @param aesKeyForSender 발신자용 AES 키(RSA 암호문)
     * @param timestamp 메시지 시각
     */
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

    // ----- Getter -----
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public String getCiphertext() { return ciphertext; }
    public String getAesKeyForReceiver() { return aesKeyForReceiver; }
    public String getAesKeyForSender() { return aesKeyForSender; }
    public String getTimestamp() { return timestamp; }
}

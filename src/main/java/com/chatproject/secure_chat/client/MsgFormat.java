package com.chatproject.secure_chat.client;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MsgFormat
 *
 * 클라이언트↔서버 간 JSON 메시지 포맷.
 *
 * ✅ 주요 원칙
 * - 본문(msg)은 "평문"이 아니라 AES로 암호화된 "암호문(Base64)"를 담는다.
 * - 실시간 전송("message") 시, 송신자는 아래 키 2개를 함께 보낸다(서버 DB 저장용).
 *    - aesKeyForReceiver : 수신자 공개키로 RSA 암호화된 AES 키(Base64)
 *    - aesKeyForSender  : 발신자(나) 공개키로 RSA 암호화된 AES 키(Base64)
 * - history 응답 시 서버는 "요청자"가 복호화할 수 있는 키를 골라 `aesKey`에 담아 내려준다.
 *   (클라이언트는 기존처럼 getAesKey()만 사용하면 복호화 가능)
 *
 * ✅ 사용 예 (클라이언트가 메시지 보낼 때)
 * MsgFormat m = new MsgFormat();
 * m.setType("message");
 * m.setNickname(myNick);
 * m.setMsg(ciphertextBase64); // AES로 암호화된 본문
 * m.setTargetList(List.of(peerNick));
 * m.setAesKeyForReceiver(RSAUtil.encrypt(aesKeyBase64, peerPublicKey));
 * m.setAesKeyForSender(RSAUtil.encrypt(aesKeyBase64, myPublicKey));
 * m.setTimestampNow();
 *
 * ✅ 사용 예 (history 요청)
 * MsgFormat h = new MsgFormat();
 * h.setType("history");
 * h.setNickname(myNick);                // 요청자
 * h.setTargetList(List.of(peerNick));   // 상대
 * h.setTimestampNow();
 *
 * ✅ 서버 → 클라이언트 (history 응답)
 * - msg : DB의 ciphertext(Base64)
 * - aesKey : 요청자 기준으로 고른 RSA 암호문 키(= aes_key_for_receiver or aes_key_for_sender)
 * - nickname/timestamp/targetList 등 부가 정보 포함
 *
 * ⚠️ 주의
 * - iv를 쓰지 않는 버전이라 AESUtil이 IV 없이 복호화되도록 구현되어 있어야 함.
 * - Gson을 사용하므로, 필드명이 서버/클라 양쪽에서 동일해야 한다.
 */
public class MsgFormat {

    /* 공통 메타 */
    private String type;              // "message", "history", "targetList", "pubkeyRequest" 등
    private String nickname;          // 보낸 사람(요청자 또는 원본 메시지 발신자)
    private String timestamp;         // ISO/일반 문자열. 정해진 포맷이 있으면 그에 맞춰 세팅

    /* 본문/대상 */
    private String msg;               // AES 암호문(Base64) 또는 시스템 메시지 문자열
    private List<String> targetList;  // 대상 닉네임 목록 (1:1이면 보통 1개만 사용)

    /* ---- E2EE 관련 ---- */
    // 1) history 응답 등 "하나의 키"만 필요할 때 쓰는 단일 키(클라이언트는 이걸로 복호화)
    private String aesKey;            // 요청자 공개키로 RSA 암호화된 AES 키(Base64) — history 응답 등에 사용

    // 2) 실시간 전송 시 서버 DB 저장을 위해 함께 보내는 "양 방향 키"
    private String aesKeyForReceiver; // 수신자 공개키로 RSA 암호화된 AES 키(Base64)
    private String aesKeyForSender;   // 발신자(나) 공개키로 RSA 암호화된 AES 키(Base64)

    /* 생성자 */
    public MsgFormat() {}

    /** 기존 코드 호환용(단일 aesKey를 쓰던 경로) */
    public MsgFormat(String nickname, String msg, String aesKey) {
        this.nickname = nickname;
        this.msg = msg;
        this.timestamp = LocalDateTime.now().toString();
        this.aesKey = aesKey;
    }

    /* 유틸: 현재 시각으로 timestamp 세팅 */
    public void setTimestampNow() {
        this.timestamp = LocalDateTime.now().toString();
    }

    /* -------- Getter / Setter -------- */

    // type
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    // nickname
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    // timestamp
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    // msg (AES 암호문 Base64)
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }

    // targetList
    public List<String> getTargetList() { return targetList; }
    public void setTargetList(List<String> targetList) { this.targetList = targetList; }

    // 단일 aesKey (history 응답 등 요청자용 1개 키)
    public String getAesKey() { return aesKey; }
    public void setAesKey(String aesKey) { this.aesKey = aesKey; }

    // DB 저장용 양방향 키 (실시간 전송 시 함께 보냄)
    public String getAesKeyForReceiver() { return aesKeyForReceiver; }
    public void setAesKeyForReceiver(String aesKeyForReceiver) { this.aesKeyForReceiver = aesKeyForReceiver; }

    public String getAesKeyForSender() { return aesKeyForSender; }
    public void setAesKeyForSender(String aesKeyForSender) { this.aesKeyForSender = aesKeyForSender; }
}

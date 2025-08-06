package com.chatproject.secure_chat.client;

import com.chatproject.secure_chat.crypto.AESUtil;
import com.chatproject.secure_chat.crypto.RSAUtil;
import com.google.gson.Gson;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Base64;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

public class ServerMessageReader implements Runnable {
    private Socket socket;
    private PublicKey otherPublicKey; // 상대 공개키 저장용
    private PrivateKey privateKey;
    private PrintWriter printWriter;
    private String nickName;


    public final List<MsgFormat> receivedMsg = new ArrayList<>();

    public ServerMessageReader(Socket socket, PrivateKey privateKey, PrintWriter printWriter, String nickName) {
        this.socket = socket;
        this.privateKey = privateKey;
        this.printWriter = printWriter;
        this.nickName = nickName;
    }

    public PublicKey getOtherPublicKey() {
        return otherPublicKey;
    }

    public void setOtherPublicKey(PublicKey otherPublicKey) {
        this.otherPublicKey = otherPublicKey;
    }

    @Override
    public void run() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Gson gson = new Gson();

            while (true) {
                String message = br.readLine();
                if (message == null || message.equals("종료")) {
                    socket.close();
                    break;
                }

                if (message.startsWith("KEY:")) {
                    String keyString = message.substring(4);
                    byte[] keyBytes = Base64.getDecoder().decode(keyString);
                    X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
//                    this.otherPublicKey = keyFactory.generatePublic(spec);
//                    this.setOtherPublicKey(otherPublicKey);
                    PublicKey receivedKey = keyFactory.generatePublic(spec); // 🔹 new 변수로 받기
                    this.setOtherPublicKey(receivedKey);
                    System.out.println("📩 공개키 수신 완료.");
                } else if (message.startsWith("ERROR:")) {
                    System.out.println("❌ 오류: " + message.substring(6));
                } else if (message.startsWith("{")) {
                    MsgFormat msgFormat = gson.fromJson(message, MsgFormat.class);
                    System.out.println("📦 msgFormat.type = " + msgFormat.getType());


                    switch (msgFormat.getType()) {
                        case "message":
                            // 🔐 암호화된 AES 키 복호화
                            String decryptedAESKeyBase64 = RSAUtil.decrypt(msgFormat.getAesKey(), privateKey);

                            // 🔐 Base64로 인코딩된 AES 키를 복원
                            byte[] decodedKey = Base64.getDecoder().decode(decryptedAESKeyBase64);
                            SecretKeySpec secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");

                            // 🔓 복호화
                            String decryptedMsg = AESUtil.decrypt(msgFormat.getMsg(), secretKey);
                            System.out.println(msgFormat.getNickname() + ": " + decryptedMsg);
                            break;
                        case "targetList":
                            System.out.println(msgFormat.getMsg());
                            break;
                        case "history": {
                            try {
                                // ── 0) 필드 점검 ───────────────────────────────────────────
                                String from = msgFormat.getNickname();
                                String encAesKey = msgFormat.getAesKey();   // ★ 문제의 주인공
                                String cipher = msgFormat.getMsg();

                                if (encAesKey == null || encAesKey.isBlank()) {
                                    boolean sentByMe = nickName.equals(from);
                                    String note = sentByMe
                                            ? "내가 보낸 메시지(요청자용 AES 키 없음) → 내가 복호화 불가"
                                            : "상대가 보낸 메시지(aesKey 누락) → 서버 응답/필드매핑 점검 필요";

                                    System.out.println(
                                            "⚠️ history aesKey 없음 | ts=" + msgFormat.getTimestamp()
                                                    + " | from=" + from
                                                    + " | sentByMe=" + sentByMe
                                                    + " | cipherLen=" + (cipher == null ? "null" : cipher.length())
                                    );

                                    // 일단 사용자 화면에 안내만 남기고 스킵하거나 placeholder 저장
                                    MsgFormat placeholder = new MsgFormat();
                                    placeholder.setNickname(from);
                                    placeholder.setMsg("[" + note + "]");
                                    placeholder.setTargetList(List.of(nickName));
                                    placeholder.setType("history");
                                    placeholder.setTimestamp(msgFormat.getTimestamp());
                                    receivedMsg.add(placeholder);
                                    break; // 복호화 시도하지 않고 다음 항목으로
                                }

                                // ── 1) RSA로 AES 키 복호화 ─────────────────────────────────
                                String aesKeyBase64 = RSAUtil.decrypt(encAesKey, privateKey);
                                if (aesKeyBase64 == null || aesKeyBase64.isBlank()) {
                                    throw new IllegalStateException("RSA 복호화 결과(aesKeyBase64)가 비어있음");
                                }

                                // ── 2) AES 키 복원 ────────────────────────────────────────
                                byte[] keyBytes = Base64.getDecoder().decode(aesKeyBase64);
                                SecretKeySpec secretKey2 = new SecretKeySpec(keyBytes, 0, keyBytes.length, "AES");

                                // ── 3) AES 복호화 (IV 없는 규약 가정) ─────────────────────
                                String plainText = AESUtil.decrypt(cipher, secretKey2);

                                // ── 4) 저장/출력 ─────────────────────────────────────────
                                MsgFormat decrypted = new MsgFormat();
                                decrypted.setNickname(from);
                                decrypted.setMsg(plainText);
                                decrypted.setTargetList(List.of(nickName));
                                decrypted.setType("history");
                                decrypted.setTimestamp(msgFormat.getTimestamp());

                                receivedMsg.add(decrypted);
                                System.out.println("[" + decrypted.getTimestamp() + "] " + decrypted.getNickname() + ": " + decrypted.getMsg());
                            } catch (Exception e) {
                                System.out.println("❌ history 복호화 실패: ts=" + msgFormat.getTimestamp()
                                        + ", from=" + msgFormat.getNickname()
                                        + ", aesKeyLen=" + (msgFormat.getAesKey() == null ? "null" : msgFormat.getAesKey().length()));
                                e.printStackTrace();
                            }
                            break;
                        }


                        case "pubkeyRequest":
                            // 공개키 요청을 받았을 때 처리 로직
                            String requester = msgFormat.getNickname(); // 요청자 닉네임
                            System.out.println("🔐 [" + requester + "] 님이 당신의 공개키를 요청했습니다.");

                            // 상대에게 내 공개키를 보냄
                            PublicKey myPubKey = RSAUtil.loadPublickeyFromFile(nickName); // 이건 내 공개키
                            String encodedKey = Base64.getEncoder().encodeToString(myPubKey.getEncoded());

                            printWriter.println("KEY:" + encodedKey);
                            printWriter.flush();
                            break;
                        default:
                            System.out.println("📨 시스템 메시지: " + msgFormat.getMsg());
                    }
                } else {
                    System.out.println("💬 일반 수신: " + message);
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
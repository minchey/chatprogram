package com.chatproject.secure_chat.client;

import com.chatproject.secure_chat.crypto.AESUtil;
import com.chatproject.secure_chat.crypto.RSAUtil;
import com.chatproject.secure_chat.server.ChatServer;
import com.chatproject.secure_chat.server.ClientInfo;
import com.google.gson.Gson;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.PrivateKey;
import java.util.Base64;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

public class ServerMessageReader implements Runnable {
    private Socket socket;
    private PublicKey otherPublicKey; // 상대 공개키 저장용
    private PrivateKey privateKey;
    private PrintWriter printWriter;

    public ServerMessageReader(Socket socket, PrivateKey privateKey, PrintWriter printWriter) {
        this.socket = socket;
        this.privateKey = privateKey;
        this.printWriter = printWriter;
    }

    public PublicKey getOtherPublicKey() {
        return otherPublicKey;
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
                    this.otherPublicKey = keyFactory.generatePublic(spec);
                    System.out.println("📩 공개키 수신 완료.");
                }
                else if (message.startsWith("ERROR:")) {
                    System.out.println("❌ 오류: " + message.substring(6));
                }
                else if (message.startsWith("{")) {
                    MsgFormat msgFormat = gson.fromJson(message, MsgFormat.class);
                    System.out.println("📦 msgFormat.type = " + msgFormat.getType());


                    switch (msgFormat.getType()){
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
                        case "pubkeyRequest":
                            // 공개키 요청을 받았을 때 처리 로직
                            String requester = msgFormat.getNickname(); // 요청자 닉네임
                            System.out.println("🔐 [" + requester + "] 님이 당신의 공개키를 요청했습니다.");

                            // 상대에게 내 공개키를 보냄
                            PublicKey myPubKey = RSAUtil.getPublicKey(); // 이건 내 공개키
                            String encodedKey = Base64.getEncoder().encodeToString(myPubKey.getEncoded());

                            //PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                            printWriter.println("KEY:" + encodedKey);
                            break;
                        default:
                            System.out.println("📨 시스템 메시지: " + msgFormat.getMsg());
                    }
//                    if("message".equals(msgFormat.getType())) {
//                        // 🔐 암호화된 AES 키 복호화
//                        String decryptedAESKeyBase64 = RSAUtil.decrypt(msgFormat.getAesKey(), privateKey);
//
//                        // 🔐 Base64로 인코딩된 AES 키를 복원
//                        byte[] decodedKey = Base64.getDecoder().decode(decryptedAESKeyBase64);
//                        SecretKeySpec secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
//
//                        // 🔓 복호화
//                        String decryptedMsg = AESUtil.decrypt(msgFormat.getMsg(), secretKey);
//                        System.out.println(msgFormat.getNickname() + ": " + decryptedMsg);
//                    }
//                    else if("targetList".equals(msgFormat.getType())){
//                        System.out.println(msgFormat.getMsg());
//                    }
//                    else {
//                        // 그 외 시스템 메시지나 추가 타입 처리
//                        System.out.println("📨 시스템 메시지: " + msgFormat.getMsg());
//                    }
                }

                else {
                    System.out.println("💬 일반 수신: " + message);
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

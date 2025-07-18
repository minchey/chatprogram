package com.chatproject.secure_chat.client;

import com.chatproject.secure_chat.crypto.AESUtil;
import com.chatproject.secure_chat.crypto.RSAUtil;
import com.google.gson.Gson;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.PrivateKey;
import java.util.Base64;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

public class ServerMessageReader implements Runnable {
    private Socket socket;
    private PublicKey otherPublicKey; // 상대 공개키 저장용
    private PrivateKey privateKey;

    public ServerMessageReader(Socket socket, PrivateKey privateKey) {
        this.socket = socket;
        this.privateKey = privateKey;
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

                    // 🔐 암호화된 AES 키 복호화
                    String decryptedAESKeyBase64 = RSAUtil.decrypt(msgFormat.getAesKey(), privateKey);

                    // 🔐 Base64로 인코딩된 AES 키를 복원
                    byte[] decodedKey = Base64.getDecoder().decode(decryptedAESKeyBase64);
                    SecretKeySpec secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");

                    // 🔓 복호화
                    String decryptedMsg = AESUtil.decrypt(msgFormat.getMsg(), secretKey);
                    System.out.println(msgFormat.getNickname() + ": " + decryptedMsg);
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

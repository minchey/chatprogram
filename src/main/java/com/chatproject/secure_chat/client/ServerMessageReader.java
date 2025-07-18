package com.chatproject.secure_chat.client;

import com.chatproject.secure_chat.crypto.AESUtil;
import com.google.gson.Gson;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class ServerMessageReader implements Runnable {
    private Socket socket;
    public ServerMessageReader(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Gson gson = new Gson(); //json 전체를 복호화 하면 에러, gson으로 파싱 후 메시지만 복호화

            while (true) {
                String message = br.readLine();
                if (message == null || message.equals("종료")) {
                    socket.close();
                    break;
                }
                if(message.startsWith("{")) {
                    // JSON 파싱
                    MsgFormat msgFormat = gson.fromJson(message, MsgFormat.class);

                    // 🔐 Base64로 인코딩된 AES 키 복원
                    byte[] decodedKey = Base64.getDecoder().decode(msgFormat.getAesKey());
                    SecretKeySpec secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");

                    // 🔓 메시지 복호화
                    String decryptedMsg = AESUtil.decrypt(msgFormat.getMsg(), secretKey);

                    System.out.println(msgFormat.getNickname() + ": " + decryptedMsg); //사용자에게 보기 좋게 출력
                }else System.out.println("서버로부터 수신: " + message);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

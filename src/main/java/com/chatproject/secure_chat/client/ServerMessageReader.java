package com.chatproject.secure_chat.client;

import com.chatproject.secure_chat.crypto.AESUtil;
import com.google.gson.Gson;

import javax.crypto.SecretKey;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class ServerMessageReader implements Runnable {
    private Socket socket;
    private final SecretKey secretKey;

    public ServerMessageReader(Socket socket, SecretKey secretKey) {
        this.socket = socket;
        this.secretKey = secretKey;
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
                MsgFormat msgFormat = gson.fromJson(message, MsgFormat.class); //메세지만 뽑아오기
                String decryptedMsg = AESUtil.decrypt(msgFormat.getMsg(), secretKey); //복호화

                System.out.println(msgFormat.getNickname() + ": " + decryptedMsg); //사용자에게 보기 좋게 출력
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

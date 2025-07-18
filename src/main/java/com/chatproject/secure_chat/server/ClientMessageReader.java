package com.chatproject.secure_chat.server; //서버스레드

import com.chatproject.secure_chat.client.MsgFormat;
import com.google.gson.Gson;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.security.PublicKey;
import java.util.Base64;
import java.net.Socket;

public class ClientMessageReader implements Runnable {

    private Socket socket;
    private String nickName;
    private PublicKey pubKey;

    public ClientMessageReader(Socket socket, String nickName, PublicKey pubKey) {
        this.socket = socket;
        this.nickName = nickName;
        this.pubKey = pubKey;
    }
    Gson gson = new Gson();

    @Override
    public void run() {
        try {

            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);


            while (true) {
                String message = br.readLine();
                System.out.println("📨 수신된 메시지(raw): " + message);

                // 🔐 공개키 요청 처리
                if (message.startsWith("REQUEST_KEY:")) {
                    String targetNickname = message.substring("REQUEST_KEY:".length());
                    PublicKey targetKey = ChatServer.publicKeyMap.get(targetNickname);
                    if (targetKey != null) {
                        String encodedKey = Base64.getEncoder().encodeToString(targetKey.getEncoded());
                        writer.println("KEY:" + encodedKey);
                    } else {
                        writer.println("ERROR:상대방 공개키를 찾을 수 없습니다.");
                    }
                    continue;
                }


                if (message.startsWith("{")) {
                    try {
                        MsgFormat msg = gson.fromJson(message, MsgFormat.class);

                        // 메시지 종료 검사
                        if ("종료".equals(msg.getMsg())) break;

                        synchronized (ChatServer.clientList) {
                            for (ClientInfo client : ChatServer.clientList) {
                                if (!client.getSocket().equals(this.socket)) {
                                    client.getPw().println(message);
                                }
                            }
                        }

                        System.out.println(nickName + ": " + msg.getMsg());
                    } catch (Exception e) {
                        System.out.println("❌ JSON 파싱 실패: " + message);
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("서버로부터 수신된 일반 메시지: " + message);
                }

            }
            br.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

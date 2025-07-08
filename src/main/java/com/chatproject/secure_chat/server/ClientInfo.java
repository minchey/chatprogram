package com.chatproject.secure_chat.server;

import java.time.LocalDateTime;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.net.Socket;


public class ClientInfo {
    private String nickname;
    private LocalDateTime connectedAt;
    private Socket socket;
    private boolean isValid = true;
    private PrintWriter pw;

    public ClientInfo(String nickname, Socket socket) {
        this.nickname = nickname;
        this.socket = socket;
        this.connectedAt = LocalDateTime.now();
        try {
            this.pw = new PrintWriter(socket.getOutputStream(), true);
        } catch (Exception e) {
            System.out.println("객체 생성에 실패하였습니다!!");
            isValid = false;
        }

    }

    public boolean isValid() {
        return isValid;
    }
}

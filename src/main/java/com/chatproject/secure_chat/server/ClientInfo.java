package com.chatproject.secure_chat.server;

import java.security.PublicKey;
import java.time.LocalDateTime;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.net.Socket;
import java.util.random.RandomGenerator;

public class ClientInfo {
    private String nickname;
    private LocalDateTime connectedAt;
    private Socket socket;
    private PublicKey publicKey;
    private boolean isValid = true;
    private PrintWriter pw;

    public ClientInfo(String nickname, Socket socket, PublicKey publicKey) {
        int randomId = RandomGenerator.getDefault().nextInt(10000);
        this.nickname = nickname + '#' + randomId;
        this.socket = socket;
        this.publicKey = publicKey;
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

    public Socket getSocket() {
        return this.socket;
    }

    public PrintWriter getPw() {
        return pw;
    }

    public String getNickname(){
        return nickname;
    }

    public PublicKey getPublicKey(){
        return publicKey;
    }
}

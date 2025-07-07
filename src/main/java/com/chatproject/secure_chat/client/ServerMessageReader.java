package com.chatproject.secure_chat.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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

            while (true){
                String message = br.readLine();
                if(message ==null || message.equals("종료")) break;
                System.out.println("서버: " + message);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

package com.chatproject.secure_chat.server;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.PrintWriter;

import java.net.Socket;

public class ClientMessageReader implements Runnable {

    private Socket socket;

    public ClientMessageReader(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(),true);
            while (true) {
                String message = br.readLine();
                writer.println(message);
                if (message == null || message.equals("종료")) break;
                System.out.println(message);


            }
            br.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

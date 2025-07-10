package com.chatproject.secure_chat.server; //서버스레드

import com.chatproject.secure_chat.client.MsgFormat;
import com.google.gson.Gson;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.net.Socket;

public class ClientMessageReader implements Runnable {

    private Socket socket;

    public ClientMessageReader(Socket socket) {
        this.socket = socket;
    }
    Gson gson = new Gson();

    @Override
    public void run() {
        try {

            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);


            while (true) {
                String message = br.readLine();
                MsgFormat msg = gson.fromJson(message, MsgFormat.class);
                String msgjson = gson.toJson(msg);
                synchronized (ChatServer.clientList) {
                    for (ClientInfo client : ChatServer.clientList) {
                        if (!client.getSocket().equals(this.socket)) {

                            client.getPw().println(msgjson);
                        }
                    }
                }


                if (message == null || message.equals("종료")) break; //루프 종료문
                System.out.println(message);


            }
            br.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

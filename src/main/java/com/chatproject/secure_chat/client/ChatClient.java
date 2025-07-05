package com.chatproject.secure_chat.client;

import java.io.PrintWriter;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        Socket clientSocket = null;

        try {
            System.out.println("서버에 연결합니다");
            clientSocket = new Socket("127.0.0.1", 9999);

        } catch (Exception e) {
            e.printStackTrace();
        }
        String message = in.nextLine();
        if (clientSocket != null) {
            try {
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                writer.println(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("서버연결 오류로 메시지 전송 실패하였습니다.");
        }


    }
}

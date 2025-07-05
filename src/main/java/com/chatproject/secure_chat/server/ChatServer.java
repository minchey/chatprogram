package com.chatproject.secure_chat.server;

import java.net.Socket;
import java.net.ServerSocket;
import com.chatproject.secure_chat.client.ChatClient;


public class ChatServer {
    public static void main(String[] args){

        try{
            ServerSocket serverSocket = new ServerSocket(9999); //서버 오픈
            System.out.println("서버실행 연결대기중...");

            Socket clientSocket = serverSocket.accept(); //클라이언트 연결시도
            System.out.println("클라이언트 연결됨");

            clientSocket.close();
            serverSocket.close();
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}

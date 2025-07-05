package com.chatproject.secure_chat.server;

import java.net.Socket;
import java.net.ServerSocket;


public class ChatServer {
    public static void main(String[] args){

        try{ //예외처리
            ServerSocket serverSocket = new ServerSocket(9999); //포트번호 9999로 서버 오픈
            System.out.println("서버실행 연결대기중...");

            Socket clientSocket = serverSocket.accept(); //클라이언트 연결
            System.out.println("클라이언트 연결됨");

            clientSocket.close(); //클라이언트 연결종료
            serverSocket.close(); //서버 종료
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}

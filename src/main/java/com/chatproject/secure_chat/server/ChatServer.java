package com.chatproject.secure_chat.server;

import java.net.Socket;
import java.net.ServerSocket;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class ChatServer {
    public static void main(String[] args) {

        try { //예외처리
            ServerSocket serverSocket = new ServerSocket(9999); //포트번호 9999로 서버 오픈
            System.out.println("서버실행 연결대기중...");

            while (true) {
                Socket clientSocket = serverSocket.accept(); //클라이언트 연결
                System.out.println("클라이언트 연결됨");
                Thread thread = new Thread(new ClientMessageReader(clientSocket));
                thread.start();
            }



            serverSocket.close(); //서버 종료
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

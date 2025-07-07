package com.chatproject.secure_chat.server;

import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class ChatServer {
    public static void main(String[] args) {

        try { //예외처리
            ArrayList<String> clientList = new ArrayList<>();
            ServerSocket serverSocket = new ServerSocket(9999); //포트번호 9999로 서버 오픈
            System.out.println("서버실행 연결대기중...");
            int client = 1; //client 숫자

            while (true) {
                Socket clientSocket = serverSocket.accept(); //클라이언트 연결
                System.out.println("클라이언트 연결됨");

                String clientName = "client" + client; //클라이언트 접속시 리스트에 저장
                clientList.add(clientName); //client 추가
                System.out.println(clientName + "님 입장");
                client ++;

                Thread thread = new Thread(new ClientMessageReader(clientSocket));
                thread.start();
            }

            //serverSocket.close(); //서버 종료
        } catch (Exception e) { //오류처리
            e.printStackTrace();
        }

    }
}

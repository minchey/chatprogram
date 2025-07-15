package com.chatproject.secure_chat.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Collections;

import com.chatproject.secure_chat.client.ChatClient;
import com.chatproject.secure_chat.server.ClientInfo;

public class ChatServer {
    public static List<ClientInfo> clientList = Collections.synchronizedList(new ArrayList<>());
    public static void main(String[] args) {


        try { //예외처리
            ServerSocket serverSocket = new ServerSocket( 9999); //포트번호 9999로 서버 오픈
            System.out.println("서버실행 연결대기중...");

            while (true) {
                Socket clientSocket = serverSocket.accept();//클라이언트 연결
                BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String nickname = br.readLine();
                ClientInfo clientInfo = new ClientInfo(nickname, clientSocket);
                clientList.add(clientInfo);
                System.out.println(nickname + "님 연결됨");
                Thread thread = new Thread(new ClientMessageReader(clientSocket, nickname));
                thread.start();
            }

            //serverSocket.close(); //서버 종료
        } catch (Exception e) { //오류처리
            e.printStackTrace();
        }

    }
}

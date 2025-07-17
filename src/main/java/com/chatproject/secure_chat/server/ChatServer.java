package com.chatproject.secure_chat.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.ServerSocket;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

import com.chatproject.secure_chat.client.ChatClient;
import com.chatproject.secure_chat.server.ClientInfo;

public class ChatServer {
    public static List<ClientInfo> clientList = Collections.synchronizedList(new ArrayList<>());
    public static Map<String, PublicKey> publicKeyMap = new HashMap<>();
    public static void main(String[] args) {


        try { //예외처리
            ServerSocket serverSocket = new ServerSocket( 9999); //포트번호 9999로 서버 오픈
            System.out.println("서버실행 연결대기중...");

            while (true) {
                Socket clientSocket = serverSocket.accept();//클라이언트 연결
                BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String nickname = br.readLine(); //닉네임 받기
                String base64Key = br.readLine(); //공개키 받기

                //Base64 -> Bytes -> PublicKey 복원
                byte[] keyBytes = Base64.getDecoder().decode(base64Key);
                X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PublicKey publicKey = keyFactory.generatePublic(spec);

                //공개키 HashMap에 저장
                publicKeyMap.put(nickname, publicKey);

                ClientInfo clientInfo = new ClientInfo(nickname, clientSocket, publicKey);
                clientList.add(clientInfo);
                System.out.println(nickname + "님 연결됨");
                Thread thread = new Thread(new ClientMessageReader(clientSocket, nickname, publicKey));
                thread.start();
            }

            //serverSocket.close(); //서버 종료
        } catch (Exception e) { //예외처리
            e.printStackTrace();
        }

    }
}

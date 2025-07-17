package com.chatproject.secure_chat.client;

import java.io.BufferedReader; //네트워크 통신시에 scanner 보다 빠르고 좋음
import java.io.InputStreamReader;
import java.io.PrintWriter; //서버에 전송할때 필요

import javax.crypto.KeyGenerator;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import com.chatproject.secure_chat.crypto.AESUtil;
import com.chatproject.secure_chat.crypto.RSAUtil;
import com.chatproject.secure_chat.server.ClientInfo;
import com.google.gson.Gson;
import java.net.Socket;


public class ChatClient {
    public static void main(String[] args) {
        Socket clientSocket = null; //소켓 객체 생성
        Gson gson = new Gson(); //json 형식 만들기

        try { //예외처리
            System.out.println("서버에 연결합니다");
            clientSocket = new Socket("127.0.0.1", 9999); //호스트와 포트번호로 연결 요청

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (clientSocket != null) { //null값 아닐때 실행
            try {
                PublicKey publicKey = RSAUtil.getPublicKey(); //공개키 생성
                PrivateKey privateKey = RSAUtil.getPrivateKey(); //개인키 생성

                //AES키 생성
                KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
                keyGenerator.init(128);
                SecretKey secretKey = keyGenerator.generateKey();
                String aesKeyString = Base64.getEncoder().encodeToString(secretKey.getEncoded());

                //AES키 암호화
                String encrypted = RSAUtil.encrypt(aesKeyString,publicKey); //AES키 암호화
                String decrypted = RSAUtil.decrypt(encrypted, privateKey); //AEs키 복호화

                //서버 메시지를 수신할 스레드 실행
                ServerMessageReader servermessagereader = new ServerMessageReader(clientSocket);
                Thread thread = new Thread(servermessagereader);
                thread.start();

                //메시지 입력 (사용자 -> 서버)
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); //bufferedreader로 입력받기
                PrintWriter printwriter = new PrintWriter(clientSocket.getOutputStream(), true); //서버에 전송

                //닉네임 생성후 전송
                System.out.println("닉네임을 입력하세요: ");
                String nickname = br.readLine();
                ClientInfo clientInfo = new ClientInfo(nickname, clientSocket, publicKey);
                printwriter.println(clientInfo.getNickname());

                //공개키 서버에 전송
                String base64PubKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
                printwriter.println(base64PubKey);

                while (true) {
                    String message = br.readLine();
                    if (message == null || message.equals("종료")) {

                        break;
                    }
                    String encryptMsg = AESUtil.encrypt(message,secretKey);
                    MsgFormat msgFormat = new MsgFormat(clientInfo.getNickname(), encryptMsg, encrypted);
                    String jsonMsg = gson.toJson(msgFormat);
                    printwriter.println(jsonMsg);

                }
                //자원정리
                printwriter.close();
                br.close();
                clientSocket.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("서버연결 오류로 메시지 전송 실패하였습니다.");
        }


    }
}

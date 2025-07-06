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

        try{ //예외처리
            ServerSocket serverSocket = new ServerSocket(9999); //포트번호 9999로 서버 오픈
            System.out.println("서버실행 연결대기중...");

            Socket clientSocket = serverSocket.accept(); //클라이언트 연결
            System.out.println("클라이언트 연결됨");

            BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); //불필요한 객체 생성 방지로 반복문 밖에 작성
            PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream(),true);
             while(true) {

                String message = br.readLine();
                System.out.println(message);//서버콘솔 출력
                if(message == null || message.equals("종료")) break; //종료조건
                printWriter.println(message); //클라이언트에 다시 전송(에코)

            }
            br.close();
            clientSocket.close(); //클라이언트 연결종료
            serverSocket.close(); //서버 종료
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}

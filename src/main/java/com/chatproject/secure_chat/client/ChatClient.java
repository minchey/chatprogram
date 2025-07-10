package com.chatproject.secure_chat.client;

import java.io.BufferedReader; //네트워크 통신시에 scanner 보다 빠르고 좋음
import java.io.InputStreamReader;
import java.io.PrintWriter; //서버에 전송할때 필요
import com.google.gson.Gson;
import java.net.Socket;


public class ChatClient {
    public static void main(String[] args) {
        Socket clientSocket = null; //소켓 객체 생성
        Gson gson = new Gson();

        try { //예외처리
            System.out.println("서버에 연결합니다");
            clientSocket = new Socket("127.0.0.1", 9999); //호스트와 포트번호로 연결 요청

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (clientSocket != null) { //null값 아닐때 실행
            try {
                //서버 메시지를 수신할 스레드 실행
                ServerMessageReader servermessagereader = new ServerMessageReader(clientSocket);
                Thread thread = new Thread(servermessagereader);
                thread.start();

                //메시지 입력 (사용자 -> 서버)
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); //bufferedreader로 입력받기
                PrintWriter printwriter = new PrintWriter(clientSocket.getOutputStream(), true); //서버에 전송

                System.out.println("닉네임을 입력하세요: ");
                String nickname = br.readLine();
                printwriter.println(nickname);
                while (true) {
                    String message = br.readLine();
                    MsgFormat msgFormat = new MsgFormat(nickname, message);
                    String jsonMsg = gson.toJson(msgFormat);
                    printwriter.println(jsonMsg);
                    if (message == null || message.equals("종료")) {

                        break;
                    }
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

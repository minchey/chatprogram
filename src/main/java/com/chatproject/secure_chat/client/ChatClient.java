package com.chatproject.secure_chat.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import com.chatproject.secure_chat.auth.UserAuth;
import com.chatproject.secure_chat.crypto.AESUtil;
import com.chatproject.secure_chat.crypto.RSAUtil;
import com.chatproject.secure_chat.server.ClientInfo;
import com.google.gson.Gson;
import java.net.Socket;

public class ChatClient {
    public static void main(String[] args) {
        Socket clientSocket = null;
        Gson gson = new Gson();
        File file = new File("USER_FILE");
        ClientInfo clientInfo = null;

        try {
            System.out.println("서버에 연결합니다");
            clientSocket = new Socket("127.0.0.1", 9999);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (clientSocket != null) {
            try {
                PublicKey publicKey = RSAUtil.getPublicKey();
                PrivateKey privateKey = RSAUtil.getPrivateKey();

                // AES 키 생성
                KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
                keyGenerator.init(128);
                SecretKey secretKey = keyGenerator.generateKey();
                String aesKeyString = Base64.getEncoder().encodeToString(secretKey.getEncoded());


                // 서버 메시지를 수신할 스레드 실행
                ServerMessageReader serverMessageReader = new ServerMessageReader(clientSocket, privateKey);
                Thread thread = new Thread(serverMessageReader);
                thread.start();

                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                PrintWriter printwriter = new PrintWriter(clientSocket.getOutputStream(), true);

                //로그인 or 회원가입 선택
                System.out.println("1.회원가입 | 2.로그인");
                String choice = br.readLine();

                System.out.println("E-mail: ");
                String email = br.readLine();

                System.out.println("PassWord: ");
                String passWord = br.readLine();

                boolean success = false;

                //회원가입
                if("1".equals(choice)){
                    int randNum = (int)(Math.random() * 9000) + 1000;
                    System.out.println("닉네임을 입력해주세요: ");
                    String nickName = br.readLine();
                    String finalNickName = nickName + "#" + randNum;

                    success = UserAuth.registerUser(email,finalNickName,passWord); //유저파일에 저장

                    if(success) {
                        System.out.println("회원가입 성공 닉네임: " + finalNickName);
                    }
                    else {
                        System.out.println("회원가입 실패");
                        return;
                    }
                }

                //로그인
                else if("2".equals(choice)){
                    success = UserAuth.loginUser(email, passWord);

                    if(success){
                        System.out.println("로그인 성공!");
                        String nickName = UserAuth.getNicknameFromUserFile(email);
                        if (nickName == null) {
                            System.out.println("닉네임을 찾을 수 없습니다.");
                            return;
                        }
                        clientInfo = new ClientInfo(nickName, clientSocket,publicKey);
                        printwriter.println(clientInfo.getNickname());

                    }
                    else {
                        System.out.println("이메일 혹은 비밀번호를 확인해주세요");
                        return;
                    }
                }

                // 공개키 전송
                String base64PubKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
                printwriter.println(base64PubKey);

                // 상대 공개키 요청
                System.out.println("누구와 채팅하시겠습니까?");
                String targetNickName = br.readLine();
                printwriter.println("REQUEST_KEY:" + targetNickName);

                // otherPublicKey 받아올 때까지 대기
                while (serverMessageReader.getOtherPublicKey() == null) {
                    Thread.sleep(100); // 잠깐 기다림
                }

                //받은 공개키로 AES 키 암호화
                String encrypted = RSAUtil.encrypt(aesKeyString, serverMessageReader.getOtherPublicKey());

                // 💬 메시지 입력 루프
                while (true) {
                    String message = br.readLine();
                    if (message == null || message.equals("종료")) {
                        break;
                    }
                    String encryptMsg = AESUtil.encrypt(message, secretKey);
                    MsgFormat msgFormat = new MsgFormat(clientInfo.getNickname(), encryptMsg, encrypted);
                    String jsonMsg = gson.toJson(msgFormat);
                    printwriter.println(jsonMsg);
                }

                // 자원 정리
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

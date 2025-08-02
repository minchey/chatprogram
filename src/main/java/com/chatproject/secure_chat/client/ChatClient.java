package com.chatproject.secure_chat.client;

import java.io.*;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;

import com.chatproject.secure_chat.auth.UserAuth;
import com.chatproject.secure_chat.crypto.AESUtil;
import com.chatproject.secure_chat.crypto.RSAUtil;
import com.chatproject.secure_chat.server.ChatServer;
import com.chatproject.secure_chat.server.ClientInfo;
import com.google.gson.Gson;

import java.net.Socket;
import java.util.List;

public class ChatClient {
    public static void main(String[] args) {
        ServerMessageReader serverMessageReader = null;
        Socket clientSocket = null;
        Gson gson = new Gson();
        File file = new File("USER_FILE");
        ClientInfo clientInfo = null;
        String encrypted = null;

        //RSA 키쌍 저장변 (블록 바깥에서 선언)
        PublicKey publicKey = null;
        PrivateKey privateKey = null;

        try {
            System.out.println("서버에 연결합니다");
            clientSocket = new Socket("127.0.0.1", 9999);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (clientSocket != null) {
            try {
                // AES 키 생성
                KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
                keyGenerator.init(128);
                SecretKey secretKey = keyGenerator.generateKey();
                String aesKeyString = Base64.getEncoder().encodeToString(secretKey.getEncoded());

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
                if ("1".equals(choice)) {
                    KeyPair newKeyPair = RSAUtil.generateKeyPair();
                    publicKey = newKeyPair.getPublic();
                    privateKey = newKeyPair.getPrivate();
                    int randNum = (int) (Math.random() * 9000) + 1000;
                    System.out.println("닉네임을 입력해주세요: ");
                    String nickName = br.readLine();
                    String finalNickName = nickName + "#" + randNum;

                    success = UserAuth.registerUser(email, finalNickName, passWord); //유저파일에 저장

                    if (success) {
                        //키파일 저장
                        RSAUtil.privateKeyToFile(finalNickName,privateKey);
                        RSAUtil.publicKeyToFile(finalNickName,publicKey);
                        System.out.println("회원가입 성공 닉네임: " + finalNickName);

                        clientInfo = new ClientInfo(finalNickName, clientSocket, publicKey);
                        printwriter.println(clientInfo.getNickname());
                        serverMessageReader = new ServerMessageReader(clientSocket, privateKey, printwriter,nickName);
                        Thread thread = new Thread(serverMessageReader);
                        thread.start();
                    } else {
                        System.out.println("회원가입 실패");
                        return;
                    }
                }

                //로그인
                else if ("2".equals(choice)) {
                    success = UserAuth.loginUser(email, passWord);
                    if (success) {
                        System.out.println("로그인 성공!");
                        String nickName = UserAuth.getNicknameFromUserFile(email);
                        publicKey = RSAUtil.loadPublickeyFromFile(nickName);
                        privateKey = RSAUtil.loadPrivateKeyFromFile(nickName);
                        if (nickName == null) { //null값 확인
                            System.out.println("닉네임을 찾을 수 없습니다.");
                            return;
                        }
                        clientInfo = new ClientInfo(nickName, clientSocket, publicKey);
                        printwriter.println(clientInfo.getNickname());
                        // 서버 메시지를 수신할 스레드 실행
                        serverMessageReader = new ServerMessageReader(clientSocket, privateKey, printwriter, nickName);
                        Thread thread = new Thread(serverMessageReader);
                        thread.start();

                    } else {
                        System.out.println("이메일 혹은 비밀번호를 확인해주세요");
                        return;
                    }
                } else {
                    System.out.println("잘못된 선택입니다.");
                    return;
                }



                // 공개키 전송
                String base64PubKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
                printwriter.println(base64PubKey);

                //접속자 리스트 요청 루프
                String targetNickname = null;
                while (targetNickname == null || targetNickname.isBlank()) {
                    System.out.println("'LIST'를 입력하면 현재 접속자 목록을 볼 수 있습니다.");
                    String input = br.readLine();

                    if (input.equalsIgnoreCase("LIST")) {
                        MsgFormat listRequest = new MsgFormat();
                        listRequest.setType("targetListRequest"); //타입지정
                        listRequest.setNickname(clientInfo.getNickname()); //요청자 닉네임

                        printwriter.println(gson.toJson(listRequest));
                        Thread.sleep(500);
                        continue;
                    }

                    //입력이 LIST가 아닐시 메세지로 판단
                    targetNickname = input;

                    //공개키 요청 전송
                    MsgFormat keyRequest = new MsgFormat();
                    keyRequest.setType("pubkeyRequest");
                    keyRequest.setNickname(clientInfo.getNickname());
                    keyRequest.setMsg(targetNickname); //요청 대상 닉네임

                    printwriter.println(gson.toJson(keyRequest));
                }

                // otherPublicKey 받아올 때까지 대기
                while (serverMessageReader.getOtherPublicKey() == null) {
                    Thread.sleep(100); // 잠깐 기다림
                }

                // DB 기반으로 변경: 이전 메시지 요청
                MsgFormat historyRequest = new MsgFormat();
                historyRequest.setType("history");
                historyRequest.setNickname(clientInfo.getNickname()); // 요청자
                historyRequest.setTargetList(List.of(targetNickname)); // 대화 상대

                printwriter.println(gson.toJson(historyRequest)); //서버에 전송
                System.out.println("🗂 이전 대화기록 요청 전송 완료");


                //받은 공개키로 AES 키 암호화
                encrypted = RSAUtil.encrypt(aesKeyString, serverMessageReader.getOtherPublicKey());


                // 💬 메시지 입력 루프
                while (true) {
                    System.out.println("🟡 메시지 입력 대기 중...");

                    String message = br.readLine();
                    System.out.println("✍️ 입력한 메시지: " + message);

                    if (message == null || message.equals("종료")) {
                        System.out.println("🔴 입력이 null이라 종료");

                        break;
                    }
                    try {

                        String encryptMsg = AESUtil.encrypt(message, secretKey);
                        MsgFormat msgFormat = new MsgFormat(clientInfo.getNickname(), encryptMsg, encrypted);
                        msgFormat.setType("message");
                        msgFormat.setTargetList(List.of(targetNickname));
                        String jsonMsg = gson.toJson(msgFormat);
                        printwriter.println(jsonMsg);
                        System.out.println("전송 완료");
                    } catch (Exception e) {
                        System.out.println("🔴 암호화 or 전송 실패!");
                        e.printStackTrace();
                    }


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

package com.chatproject.secure_chat.client;

import java.io.*;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.net.Socket;
import java.util.Base64;
import java.util.List;

import com.chatproject.secure_chat.auth.UserAuth;
import com.chatproject.secure_chat.crypto.AESUtil;
import com.chatproject.secure_chat.crypto.RSAUtil;
import com.chatproject.secure_chat.server.ClientInfo;
import com.google.gson.Gson;

public class ChatClient {
    public static void main(String[] args) {
        ServerMessageReader serverMessageReader = null;
        Socket clientSocket = null;
        Gson gson = new Gson();
        ClientInfo clientInfo = null;
        String encrypted = null;

        PublicKey publicKey = null; //공개키 초기와
        PrivateKey privateKey = null; //개인키 초기화

        try {
            System.out.println("서버에 연결합니다"); //서버연결 로그
            clientSocket = new Socket("127.0.0.1", 9999); // 서버 연결
            //System.out.println(clientSocket);
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

                System.out.println("1.회원가입 | 2.로그인");
                String choice = br.readLine();

                System.out.println("E-mail: ");
                String email = br.readLine();

                System.out.println("PassWord: ");
                String passWord = br.readLine();

                boolean success = false;

                // 회원가입 처리
                if ("1".equals(choice)) {
                    KeyPair newKeyPair = RSAUtil.generateKeyPair();
                    publicKey = newKeyPair.getPublic();
                    privateKey = newKeyPair.getPrivate();

                    int randNum = (int) (Math.random() * 9000) + 1000;
                    System.out.println("닉네임을 입력해주세요: ");
                    String nickName = br.readLine();
                    String finalNickName = nickName + "#" + randNum;

                    success = UserAuth.registerUser(email, finalNickName, passWord);

                    if (success) {
                        RSAUtil.privateKeyToFile(finalNickName, privateKey);
                        RSAUtil.publicKeyToFile(finalNickName, publicKey);

                        System.out.println("회원가입 성공 닉네임: " + finalNickName);

                        clientInfo = new ClientInfo(finalNickName, clientSocket, publicKey);


                        // 수신 스레드 실행
                        serverMessageReader = new ServerMessageReader(clientSocket, privateKey, printwriter, finalNickName);
                        Thread thread = new Thread(serverMessageReader);
                        thread.start();

                        // 닉네임 전송
                        printwriter.println(clientInfo.getNickname());
                    } else {
                        System.out.println("회원가입 실패");
                        return;
                    }
                }
                // 로그인 처리
                else if ("2".equals(choice)) {
                    success = UserAuth.loginUser(email, passWord);
                    if (success) {
                        System.out.println("로그인 성공!");
                        String nickName = UserAuth.getNicknameFromUserFile(email);
                        publicKey = RSAUtil.loadPublickeyFromFile(nickName);
                        privateKey = RSAUtil.loadPrivateKeyFromFile(nickName);

                        if (nickName == null) {
                            System.out.println("닉네임을 찾을 수 없습니다.");
                            return;
                        }
                        clientInfo = new ClientInfo(nickName, clientSocket, publicKey);
                        //System.out.println(clientInfo.getSocket());

                        // 수신 스레드 실행
                        serverMessageReader = new ServerMessageReader(clientSocket, privateKey, printwriter, nickName);
                        Thread thread = new Thread(serverMessageReader);
                        thread.start();

                        // 닉네임 전송
                        printwriter.println(clientInfo.getNickname());
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

                // 공개키 요청 전 약간의 딜레이를 줘야 서버 수신 준비 완료됨
                Thread.sleep(300);

                // 대상 선택 및 공개키 요청 루프
                String targetNickname = null;
                while (targetNickname == null || targetNickname.isBlank()) {
                    System.out.println("'LIST'를 입력하면 현재 접속자 목록을 볼 수 있습니다.");
                    System.out.println("대화 상대를 입력해주세요: ");
                    String input = br.readLine();

                    if (input.equalsIgnoreCase("LIST")) {
                        MsgFormat listRequest = new MsgFormat();
                        listRequest.setType("targetListRequest");
                        listRequest.setNickname(clientInfo.getNickname());
                        printwriter.println(gson.toJson(listRequest));
                        Thread.sleep(500);
                        continue;
                    }

                    targetNickname = input;

                    MsgFormat keyRequest = new MsgFormat();
                    keyRequest.setType("pubkeyRequest");
                    keyRequest.setNickname(clientInfo.getNickname());
                    keyRequest.setMsg(targetNickname);
                    printwriter.println(gson.toJson(keyRequest));
                }

                // 공개키 수신 대기
                while (serverMessageReader.getOtherPublicKey() == null) {
                    System.out.println(".");
                    Thread.sleep(100);
                }

                // 이전 메시지 요청
                MsgFormat historyRequest = new MsgFormat();
                historyRequest.setType("history");
                historyRequest.setNickname(clientInfo.getNickname());
                historyRequest.setTargetList(List.of(targetNickname));
                printwriter.println(gson.toJson(historyRequest));
                printwriter.flush();
                System.out.println("\uD83D\uDDC2 이전 대화기록 요청 전송 완료");

                // AES 키를 상대 공개키로 암호화
                encrypted = RSAUtil.encrypt(aesKeyString, serverMessageReader.getOtherPublicKey());

                // 메시지 입력 루프
                while (true) {
                    System.out.println("\uD83D\uDFE1 메시지 입력 대기 중...");
                    String message = br.readLine();
                    System.out.println("\u270D️ 입력한 메시지: " + message);

                    if (message == null || message.equals("종료")) {
                        System.out.println("\uD83D\uDD34 입력이 null이라 종료");
                        break;
                    }

                    try {
                        String encryptMsg = AESUtil.encrypt(message, secretKey);
                        MsgFormat msgFormat = new MsgFormat(clientInfo.getNickname(), encryptMsg, encrypted);
                        msgFormat.setType("message");
                        msgFormat.setTargetList(List.of(targetNickname));
                        printwriter.println(gson.toJson(msgFormat));
                        System.out.println("전송 완료");
                    } catch (Exception e) {
                        System.out.println("\uD83D\uDD34 암호화 or 전송 실패!");
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

package com.chatproject.secure_chat.client;

import java.io.*;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.MessageFormat;
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
        Socket clientSocket = null;
        Gson gson = new Gson();
        File file = new File("USER_FILE");
        ClientInfo clientInfo = null;

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

                        return;
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
                        ServerMessageReader serverMessageReader = new ServerMessageReader(clientSocket, privateKey, printwriter);
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

                List<MsgFormat> receivedMessaged = new ArrayList<>(); //수신한 메시지 저장할 리스트
                //이전 대화로그 불러오기
                String myNickname = clientInfo.getNickname(); //현재 사용자 닉네임

                String[] names = {myNickname, targetNickname}; //본인,상대 닉네임 배열
                Arrays.sort(names); //파일명 일관성 위해 배열정렬
                String fileName = names[0] + "&" + names[1] + ".log"; //파일이름 재구성해서 파일 찾기

                File logFile = new File("Message_Logs", fileName); //로그파일이 저장된 디렉토리 경로와 파일명 조합해서 File 객체 생성
                if (logFile.exists()) { //해당 로그파일이 존재하는지 확인
                    System.out.println("이전 대화기록:");
                    BufferedReader logReader = new BufferedReader(new FileReader(logFile)); //파일을 한줄씩 읽기위한 BufferedReader
                    String line;

                    //파일을 끝까지 반복해서 한 줄씩 출력
                    while ((line = logReader.readLine()) != null) {
                        int jsonStart = line.indexOf("{");
                        if (jsonStart != -1) { //{가 없으면 Json이 아님
                            String jsonPart = line.substring(jsonStart); //Json 부분만 추출

                            try {
                                MsgFormat msg = gson.fromJson(jsonPart, MsgFormat.class);

                                //AES키 복호화
                                String decryptedAESKeyBase64 = RSAUtil.decrypt(msg.getAesKey(), privateKey);

                                //Base64 디코딩해서 AES키 복원
                                byte[] decodedKey = Base64.getDecoder().decode(decryptedAESKeyBase64);
                                SecretKeySpec secretKeySpec = new SecretKeySpec(decodedKey, "AES");

                                //메시지 복호화
                                String decryptedMsg = AESUtil.decrypt(msg.getMsg(), secretKeySpec);
                                System.out.println("[" + msg.getNickname() + "] " + decryptedMsg);
                            } catch (Exception e) {
                                System.out.println(" 복호화 실패한 로그: " + line);
                            }

                        }
                    }

                    //자원정리
                    logReader.close();
                } else {
                    System.out.println("이전 대화 기록 없음");
                }

                //받은 공개키로 AES 키 암호화
                String encrypted = RSAUtil.encrypt(aesKeyString, serverMessageReader.getOtherPublicKey());


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

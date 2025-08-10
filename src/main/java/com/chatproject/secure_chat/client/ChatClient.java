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

        // ─────────────────────────────────────────────────────────────
        // 🧩 도커/서버 환경 대응: 환경변수로 서버 호스트/포트 받기
        //    (도커 컴포즈나 docker run -e SERVER_HOST=secure-server 식으로 설정)
        //    기본값은 127.0.0.1:9999
        // ─────────────────────────────────────────────────────────────
        final String SERVER_HOST = System.getenv().getOrDefault("SERVER_HOST", "127.0.0.1");
        final int SERVER_PORT = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "9999"));

        String encryptedForReceiver = null; // 수신자용 RSA 암호문 AES키 (전송용)
        String encryptedForSender = null;   // 발신자(나)용 RSA 암호문 AES키 (DB 저장/히스토리 복구용)

        PublicKey publicKey = null;  // 공개키
        PrivateKey privateKey = null; // 개인키

        try {
            System.out.println("서버에 연결합니다 → " + SERVER_HOST + ":" + SERVER_PORT);
            clientSocket = new Socket(SERVER_HOST, SERVER_PORT); // 서버 연결
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (clientSocket != null) {
            try {
                // ─────────────────────────────────────────────────────────────
                // 🔐 세션용 AES 키 생성 (이 세션에서 메시지 암/복호화에 사용)
                //    - message/history 모두 이 키로 암호화/복호화 (키 자체는 RSA로 포장되어 전송/저장)
                // ─────────────────────────────────────────────────────────────
                KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
                keyGenerator.init(128);
                SecretKey secretKey = keyGenerator.generateKey();
                String aesKeyBase64 = Base64.getEncoder().encodeToString(secretKey.getEncoded());

                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                PrintWriter printwriter = new PrintWriter(clientSocket.getOutputStream(), true);

                System.out.println("1.회원가입 | 2.로그인");
                String choice = br.readLine();

                System.out.println("E-mail: ");
                String email = br.readLine();

                System.out.println("PassWord: ");
                String passWord = br.readLine();

                boolean success = false;

                // 회원가입
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

                        // 수신 스레드 실행 (내 개인키로 서버에서 오는 aesKey 복호화에 사용됨)
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
                // 로그인
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

                // ─────────────────────────────────────────────────────────────
                // 📤 내 공개키를 서버에 전송 (현 프로토콜 유지: 평문 Base64)
                // ─────────────────────────────────────────────────────────────
                String base64PubKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
                printwriter.println(base64PubKey);

                // 공개키 요청 전에 약간의 딜레이 (서버 준비 시간)
                Thread.sleep(300);

                // ─────────────────────────────────────────────────────────────
                // 🎯 상대 선택 & 공개키 요청 루프
                // ─────────────────────────────────────────────────────────────
                String targetNickname = null;
                while (targetNickname == null || targetNickname.isBlank()) {
                    System.out.println("'LIST'를 입력하면 현재 접속자 목록을 볼 수 있습니다.");
                    System.out.println("대화 상대를 입력해주세요: ");
                    String input = br.readLine();

                    if (input == null) {
                        System.out.println("입력이 null입니다. 종료합니다.");
                        return;
                    }

                    if (input.equalsIgnoreCase("LIST")) {
                        MsgFormat listRequest = new MsgFormat();
                        listRequest.setType("targetListRequest");
                        listRequest.setNickname(clientInfo.getNickname());
                        printwriter.println(gson.toJson(listRequest));
                        Thread.sleep(500);
                        continue;
                    }

                    targetNickname = input;

                    // 상대 공개키 요청
                    MsgFormat keyRequest = new MsgFormat();
                    keyRequest.setType("pubkeyRequest");
                    keyRequest.setNickname(clientInfo.getNickname());
                    keyRequest.setMsg(targetNickname); // 현재 프로토콜: msg에 대상 닉네임
                    printwriter.println(gson.toJson(keyRequest));
                }

                // 상대 공개키 수신 대기
                while (serverMessageReader.getOtherPublicKey() == null) {
                    System.out.println(".");
                    Thread.sleep(100);
                }
                PublicKey receiverPublicKey = serverMessageReader.getOtherPublicKey();

                // ─────────────────────────────────────────────────────────────
                // 🗂 히스토리 요청 (요청자는 나, 대상은 targetNickname)
                // ─────────────────────────────────────────────────────────────
                MsgFormat historyRequest = new MsgFormat();
                historyRequest.setType("history");
                historyRequest.setNickname(clientInfo.getNickname());
                historyRequest.setTargetList(List.of(targetNickname));
                printwriter.println(gson.toJson(historyRequest));
                printwriter.flush();
                System.out.println("🗂 이전 대화기록 요청 전송 완료");

                // ─────────────────────────────────────────────────────────────
                // 🔑 AES 키를 수신자/발신자 공개키로 각각 암호화해 둔다
                //    - encryptedForReceiver: 상대가 복호화 가능
                //    - encryptedForSender : 내가 나중에 히스토리 복구 시 내가 복호화 가능
                // ─────────────────────────────────────────────────────────────
                encryptedForReceiver = RSAUtil.encrypt(aesKeyBase64, receiverPublicKey);
                encryptedForSender = RSAUtil.encrypt(aesKeyBase64, publicKey);

                // ─────────────────────────────────────────────────────────────
                // ✉️ 메시지 입력/전송 루프
                // ─────────────────────────────────────────────────────────────
                while (true) {
                    System.out.println("🟡 메시지 입력 대기 중...");
                    String message = br.readLine();
                    System.out.println("✍️ 입력한 메시지: " + message);

                    if (message == null || message.equals("종료")) {
                        System.out.println("🔴 입력이 null/종료. 클라이언트 종료");
                        break;
                    }

                    try {
                        // AES로 본문 암호화 (Base64 암호문)
                        String encryptMsg = AESUtil.encrypt(message, secretKey);

                        // 메시지 포맷 구성
                        // ✅ 중요: 서버가 DB에 두 종류의 aesKey를 저장할 수 있도록 함께 보냄
                        MsgFormat msgFormat = new MsgFormat(clientInfo.getNickname(), encryptMsg, encryptedForReceiver);
                        msgFormat.setType("message");
                        msgFormat.setTargetList(List.of(targetNickname));

                        // 아래 필드는 확장된 MsgFormat(앞서 추가한 getter/setter 필요)
                        msgFormat.setAesKeyForReceiver(encryptedForReceiver);
                        msgFormat.setAesKeyForSender(encryptedForSender);

                        printwriter.println(gson.toJson(msgFormat));
                        System.out.println("✅ 전송 완료");
                    } catch (Exception e) {
                        System.out.println("🔴 암호화/전송 실패!");
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

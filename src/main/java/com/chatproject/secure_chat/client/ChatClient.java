package com.chatproject.secure_chat.client;

import java.io.*;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
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

/**
 * ChatClient
 *
 * <역할>
 * - 서버에 접속해서 로그인/회원가입 진행
 * - 상대 공개키를 받아 세션 AES 키를 RSA로 포장하여 송신
 * - 실시간 메시지 전송 + history 요청/수신
 *
 * <프로토콜 요약>
 * - msg: AES 암호문(Base64)
 * - message 전송 시: aesKeyForReceiver(상대 공개키로 RSA 암호화), aesKeyForSender(내 공개키로 RSA 암호화)
 * - history 요청 후 서버 응답: aesKey(요청자 기준으로 선택된 RSA 암호문 AES 키)
 *
 * <도커/환경>
 * - SERVER_HOST, SERVER_PORT 환경변수로 서버 주소 설정(기본 127.0.0.1:9999)
 *
 * <변경 요약>
 * - 환경변수 지원, flush 보장, 공개키 수신 타임아웃, 입력 EOF 처리, 안정성 로그
 */
public class ChatClient {
    public static void main(String[] args) {
        ServerMessageReader serverMessageReader = null;
        Socket clientSocket = null;
        Gson gson = new Gson();
        ClientInfo clientInfo = null;

        // ─────────────────────────────────────────────────────────────
        //  도커/서버 환경 대응: 환경변수로 서버 호스트/포트 받기 (기본 127.0.0.1:9999)
        //    docker run -e SERVER_HOST=secure-server -e SERVER_PORT=9999 ...
        // ─────────────────────────────────────────────────────────────
        final String SERVER_HOST = System.getenv().getOrDefault("SERVER_HOST", "127.0.0.1");
        final int SERVER_PORT = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "9999"));

        // 수신자/발신자용 RSA 암호문 AES 키 (전송/DB 저장/히스토리 복구 용도)
        String encryptedForReceiver = null;
        String encryptedForSender = null;

        PublicKey publicKey = null;   // 내 공개키
        PrivateKey privateKey = null; // 내 개인키

        try {
            System.out.println("서버에 연결합니다 → " + SERVER_HOST + ":" + SERVER_PORT);
            clientSocket = new Socket(SERVER_HOST, SERVER_PORT);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (clientSocket != null) {
            try {
                // ─────────────────────────────────────────────────────────────
                // 🔐 세션용 AES 키 생성
                //   모든 메시지는 이 키로 AES 암호화
                //    키 자체는 RSA(상대/나)로 포장하여 전송/DB 저장 → 히스토리 복구 가능
                // ─────────────────────────────────────────────────────────────
                KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
                keyGenerator.init(128);
                SecretKey secretKey = keyGenerator.generateKey();
                String aesKeyBase64 = Base64.getEncoder().encodeToString(secretKey.getEncoded());

                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                PrintWriter printwriter = new PrintWriter(clientSocket.getOutputStream(), true);

                // ───────────────── 로그인/회원가입 ─────────────────
                System.out.println("1.회원가입 | 2.로그인");
                String choice = br.readLine();
                if (choice == null) {
                    System.out.println("입력이 종료되었습니다.");
                    return;
                }

                System.out.println("E-mail: ");
                String email = br.readLine();
                if (email == null) {
                    System.out.println("입력이 종료되었습니다.");
                    return;
                }

                System.out.println("PassWord: ");
                String passWord = br.readLine();
                if (passWord == null) {
                    System.out.println("입력이 종료되었습니다.");
                    return;
                }

                boolean success = false;

                if ("1".equals(choice)) {
                    // 회원가입
                    KeyPair newKeyPair = RSAUtil.generateKeyPair();
                    publicKey = newKeyPair.getPublic();
                    privateKey = newKeyPair.getPrivate();

                    int randNum = (int) (Math.random() * 9000) + 1000;
                    System.out.println("닉네임을 입력해주세요: ");
                    String nickName = br.readLine();
                    if (nickName == null) {
                        System.out.println("입력이 종료되었습니다.");
                        return;
                    }
                    String finalNickName = nickName + "#" + randNum;

                    success = UserAuth.registerUser(email, finalNickName, passWord);
                    if (success) {
                        RSAUtil.privateKeyToFile(finalNickName, privateKey);
                        RSAUtil.publicKeyToFile(finalNickName, publicKey);

                        System.out.println("회원가입 성공 닉네임: " + finalNickName);

                        clientInfo = new ClientInfo(finalNickName, clientSocket, publicKey);

                        // 수신 스레드 (서버가 주는 aesKey 복호화에 내 개인키 사용)
                        serverMessageReader = new ServerMessageReader(clientSocket, privateKey, printwriter, finalNickName);
                        Thread thread = new Thread(serverMessageReader);
                        thread.start();

                        // 닉네임 전송
                        printwriter.println(clientInfo.getNickname());
                        printwriter.flush();
                    } else {
                        System.out.println("회원가입 실패");
                        return;
                    }
                } else if ("2".equals(choice)) {
                    // 로그인
                    success = UserAuth.loginUser(email, passWord);
                    if (success) {
                        System.out.println("로그인 성공!");
                        String nickName = UserAuth.getNicknameFromUserFile(email);
                        if (nickName == null) {
                            System.out.println("닉네임을 찾을 수 없습니다.");
                            return;
                        }
                        publicKey = RSAUtil.loadPublickeyFromFile(nickName);
                        privateKey = RSAUtil.loadPrivateKeyFromFile(nickName);

                        clientInfo = new ClientInfo(nickName, clientSocket, publicKey);

                        // 수신 스레드 실행
                        serverMessageReader = new ServerMessageReader(clientSocket, privateKey, printwriter, nickName);
                        Thread thread = new Thread(serverMessageReader);
                        thread.start();

                        // 닉네임 전송
                        printwriter.println(clientInfo.getNickname());
                        printwriter.flush();
                    } else {
                        System.out.println("이메일 혹은 비밀번호를 확인해주세요");
                        return;
                    }
                } else {
                    System.out.println("잘못된 선택입니다.");
                    return;
                }

                // ─────────────────────────────────────────────────────────────
                // 📤 내 공개키를 서버에 전송 (현 프로토콜 그대로: 평문 Base64 한 줄)
                // ─────────────────────────────────────────────────────────────
                String base64PubKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
                printwriter.println(base64PubKey);
                printwriter.flush();

                // 공개키 요청 전에 약간의 딜레이 (서버 수신 준비)
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
                        System.out.println("입력이 종료되었습니다.");
                        return;
                    }

                    if (input.equalsIgnoreCase("LIST")) {
                        MsgFormat listRequest = new MsgFormat();
                        listRequest.setType("targetListRequest");
                        listRequest.setNickname(clientInfo.getNickname());
                        printwriter.println(gson.toJson(listRequest));
                        printwriter.flush();
                        Thread.sleep(500);
                        continue;
                    }

                    targetNickname = input;

                    // 상대 공개키 요청
                    MsgFormat keyRequest = new MsgFormat();
                    keyRequest.setType("pubkeyRequest");
                    keyRequest.setNickname(clientInfo.getNickname());
                    keyRequest.setMsg(targetNickname);
                    printwriter.println(gson.toJson(keyRequest));
                    printwriter.flush();
                }

                // 상대 공개키 수신 대기
                long waitStart = System.currentTimeMillis();
                while (serverMessageReader.getOtherPublicKey() == null) {
                    if (System.currentTimeMillis() - waitStart > 10_000) {
                        System.out.println("⛔ 상대 공개키 수신 타임아웃");
                        return;
                    }
                    Thread.sleep(100);
                }
                PublicKey receiverPublicKey = serverMessageReader.getOtherPublicKey();
                if (receiverPublicKey == null) {
                    System.out.println("⛔ 상대 공개키가 없습니다. 종료합니다.");
                    return;
                }

                // ─────────────────────────────────────────────────────────────
                // 🗂 히스토리 요청
                // ─────────────────────────────────────────────────────────────
                MsgFormat historyRequest = new MsgFormat();
                historyRequest.setType("history");
                historyRequest.setNickname(clientInfo.getNickname());
                historyRequest.setTargetList(List.of(targetNickname));
                printwriter.println(gson.toJson(historyRequest));
                printwriter.flush();
                System.out.println("🗂 이전 대화기록 요청 전송 완료");

                // ─────────────────────────────────────────────────────────────
                // 🔑 AES 키를 수신자/발신자 공개키로 각각 RSA 암호화(미리 준비)
                //    - encryptedForReceiver: 상대가 복호화 가능(수신자용)
                //    - encryptedForSender : 내가 복호화 가능(발신자용, 히스토리 복구용)
                // ─────────────────────────────────────────────────────────────
                encryptedForReceiver = RSAUtil.encrypt(aesKeyBase64, receiverPublicKey);
                encryptedForSender  = RSAUtil.encrypt(aesKeyBase64, publicKey);

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
                        // ✅ 서버가 DB에 두 종류의 aesKey를 저장할 수 있도록 함께 보냄
                        MsgFormat msgFormat = new MsgFormat(clientInfo.getNickname(), encryptMsg, encryptedForReceiver);
                        msgFormat.setType("message");
                        msgFormat.setTargetList(List.of(targetNickname));
                        msgFormat.setAesKeyForReceiver(encryptedForReceiver);
                        msgFormat.setAesKeyForSender(encryptedForSender);

                        printwriter.println(gson.toJson(msgFormat));
                        printwriter.flush();
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
